/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editore.
 */
package mqttswitch;

import de.horatio.common.HoraIni;
import de.horatio.common.HoraTime;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.logging.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author duemchen
 */
public class MQTTSwitch implements MqttCallback {

    /**
     *
     * @author duemchen
     */
    private static Logger log = Logger.getLogger("MQTTSwitchLogger");
    private static boolean stop;

    MqttAsyncClient client;
    private String lastMessage = "";
    private boolean connectionOK;
    private boolean switchMessage = false;
    SwitchCallback callback;
    private final String INI = "mqttswitch.ini";
    private String MQTTLINK = "duemchen.feste-ip.net:56686";
    private static long lastPiep = 0;

    public MQTTSwitch() {
        DOMConfigurator.configureAndWatch("log4j.xml", 5 * 1000);
    }

    public void register(SwitchCallback callback) {

        this.callback = callback;
    }

    public void connectToMQTT() throws InterruptedException {
        Thread.sleep(3000);
        try {
            //
            MQTTLINK = HoraIni.LeseIniString(INI, "MQTT", "LINK_PORT", MQTTLINK, true);
            log.info(MQTTLINK);
            MQTTLINK = "tcp://" + MQTTLINK;
            //
            MemoryPersistence persistence = new MemoryPersistence();
            // jeder client muss eine zufallsid generieren, um stress zu vermeiden
            SecureRandom random = new SecureRandom();
            String id = new BigInteger(60, random).toString(32);
            System.out.println();
            System.out.println("id=" + id);
            client = new MqttAsyncClient(MQTTLINK, id, persistence);
            client.connect();
            Thread.sleep(1000);
            client.setCallback(this);
            //client.subscribe("home/OpenMQTTGateway_ESP32_RF/433toMQTT/#", 0);
            client.subscribe("433Simple/#", 0);
//            client.subscribe("sonnen/#");
//            client.subscribe("simago/test");
//            client.subscribe("simago/veranda");
//            client.subscribe("simago/elektro");
            // client.subscribe("simago/compass/+");
            // http://mosquitto.org/man/mqtt-7.html  + nur die macs, # auch die root
            connectionOK = true;
            System.out.println("connected.");
            MqttMessage echo = new MqttMessage(("connected at " + HoraTime.dateOnlyToStr(new Date(), "HH:mm:ss, EE dd.MM.yy")).getBytes());
            client.publish("433/server", echo);

        } catch (MqttException e) {
            log.error("connectToMQTT", e);
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connectionOK = false;
        log.debug("connectionLost");

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        //if ("home/OpenMQTTGateway_ESP32_ALL/433toMQTT".equals(topic)) {
        System.out.println(topic + ", |||||" + message + "|||||| " + new Date());

        // if ("home/OpenMQTTGateway_ESP32_RF/433toMQTT".equals(topic)) {
        if ("433Simple/sensor".equals(topic)) {

            try {
                byte[] bb = message.getPayload();
                String s = new String(bb);
                int i = Integer.parseInt(s);
                //System.out.println("433: " + i);
                String sd = "" + HoraTime.dateOnlyToStr(new Date(), "dd.MM.yyyy HH:mm:ss.SSS");

                MqttMessage echo;
                boolean gültig = true;
                switch (i) {
                    case 7235862://9803110:
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/teich", echo);
                        break;
                    case 1228814:
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/briefkasten", echo);
                        break;

//neu // Dümchen 01.08.2020
//4053766  oben
//9803110  unten                        
                    case 4053766:
                        //case 14222350:                        
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/oben", echo);
                        //System.out.println(new Date() + "    " + topic + ", " + message);
                        break;
                    // case 1115150:
                    case 9803110:
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/unten", echo);
                        //System.out.println(new Date() + "    " + topic + ", " + message);
                        break;

                    case 14222350:
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/oben2", echo);
                        //System.out.println(new Date() + "    " + topic + ", " + message);
                        gonge();
                        break;
                    case 1115150:
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/unten2", echo);
                        //System.out.println(new Date() + "    " + topic + ", " + message);
                        gonge();
                        break;
                    default:
                        gültig = false;
                        //legt die Zahl einfach an.
                        echo = new MqttMessage(sd.getBytes());
                        client.publish("433/unknown/" + i, echo);

                }
                if (gültig) {
                    log.info(": " + i);
                }

                // logHeizung.info("" + message);
            } catch (Exception e) {
                //System.out.println(e);
                byte[] bb = message.getPayload();
                String s = new String(bb);
                int i = Integer.parseInt(s);
                //System.out.println("433: " + i);
                String sd = "" + HoraTime.dateOnlyToStr(new Date(), "dd.MM.yyyy HH:mm:ss.SSS");
                log.error(sd + " : " + s);
            }
        }
    }

    private void gonge() {
        final long step = 20 * 1000;
        long timestamp = System.currentTimeMillis();
        if (timestamp > lastPiep + step) {
            String ss = "4";
            try {
                MqttMessage echo = new MqttMessage(ss.getBytes());
                client.publish("tor/gong", echo);

            } catch (Exception e) {
                log.error("gonge " + e);
            }
        }
        lastPiep = timestamp;

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token
    ) {
        //System.out.println("delivery complete");
    }

    public String getLastMessage() {

        return lastMessage;
    }

    public boolean isConnected() {

        return connectionOK;

    }

    private static void start(String[] args) {
        System.out.println();
        System.out.println("Start MQTTSwitch.");
        log.info("Start MQTTSwitch.");

        SwitchReadThread instance = new SwitchReadThread();
        instance.setName("switchReader");
        instance.start();
        stop = false;
        while (!stop) {
            try {
                Thread.sleep(1000);
                //System.out.print(".");
            } catch (InterruptedException ex) {
                stop = true;
            }
        }
        System.out.print("Beende MQTTSwitch....");
        log.info("Stoppe MQTTSwitch...");
        instance.doStop();
        instance.interrupt();

    }

    private static void stop(String[] args) {
        System.out.print("Stop MQTTSwitch.");
        stop = true;
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        if (args.length > 0) {
            if ("start".equals(args[0])) {
                start(args);
            } else {
                if ("stop".equals(args[0])) {
                    stop(args);
                } else {
                    System.out.print("Parameter start oder stop notwendig.");
                }
            }
        } else {
            System.out.print("Parameter start oder stop notwendig.");
            start(args);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(MQTTSwitch.class.getName()).log(Level.SEVERE, null, ex);
                log.info("interrupted");
            }
            //System.out.print("stop notwendig.");
            String[] s = new String[1];
            stop(s);

        }
    }
}

/**
 *
 * Laden aus ini + topic quelle ziel für RF433 zu lesbaren topics + Auswertung
 * Richtungserkennung in Java und speichern in lesbare topics + direkte
 * Auswertung in iobroker. Dort nur noch Zeitengrenzung für Alexa
 *
 *
 * 2 sensoren werden zu richtung true false zeitstempel zur timeout erkennung
 * tuning durch lernen???
 *
 * + letzte Bewegung vor den Fewos per Sensor + Richtung der Bewegung aus
 * Torsensor und Fewo Eingangstür Sensor gibt Status "zuHause" Wenn nur Bewegung
 * und kein EinAusgang ? timeout. anwesend. um
 *
 * Anzeige
 *
 *
 */
