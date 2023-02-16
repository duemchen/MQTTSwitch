/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mqttswitch;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author duemchen
 */
public class SwitchReadThread extends Thread implements SwitchCallback {

    private static SwitchReadThread instance;
    private MQTTSwitch tr;
    private Double temperatur;
    private String time;
    private boolean stop;

    @Override
    public void run() {
        stop = false;
        while (!stop) {
            tr = new MQTTSwitch();

            tr.register(this);
            try {
                tr.connectToMQTT();

            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (tr.isConnected()) {
                try {
                    Thread.sleep(2000);

                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                if (stop) {
                    System.out.println("dostop break");
                    break;
                }
            }
            System.out.println("restart");

            try {
                if (!stop) {
                    Thread.sleep(5000);
                } else {
                    System.out.println("dostop sleep");
                }
            } catch (InterruptedException e) {

                e.printStackTrace();
            }

        }
        System.out.println("dostop end.");
        try {
            tr.client.disconnect(2000);
        } catch (MqttException ex) {
            Logger.getLogger(SwitchReadThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            tr.client.close();
        } catch (MqttException ex) {
            Logger.getLogger(SwitchReadThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static SwitchReadThread getInstance() {
        if (instance == null) {
            instance = new SwitchReadThread();
            instance.setName("switchReader");
            instance.start();
        }

        return instance;
    }

    @Override
    public void setTempValues(String message) {
        System.out.println("callback " + message);

    }

    public String getTemperatur() {

        return "" + temperatur;
    }

    public String getTimeStr() {

        return time;
    }

    void doStop() {
        System.out.println("dostop");
        stop = true;
    }
}
