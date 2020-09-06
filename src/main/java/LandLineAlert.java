import com.fazecast.jSerialComm.SerialPort;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class LandLineAlert {
    private static final String INIT_CALLER_ID = "AT+VCID=1\r";
    // COM* for Windows, /dev/tty* for Linux
    private static final String SERIAL_PORT = "/dev/ttyACM0";
    private static final int BAUD_RATE = 57600;
    
    private final SerialPort serialPort;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mma");
    
    public LandLineAlert() {
        serialPort = SerialPort.getCommPort(SERIAL_PORT);
        serialPort.setBaudRate(BAUD_RATE);
        
        serialPort.openPort();
        System.out.println("Connected to " + serialPort.getSystemPortName());
        byte[] callerIdBytes = INIT_CALLER_ID.getBytes(StandardCharsets.UTF_8);
        serialPort.writeBytes(callerIdBytes, callerIdBytes.length);
    }

    public void run() throws InterruptedException, IOException, FirebaseMessagingException {
        InputStream inputStream = serialPort.getInputStream();
        while (true) {
            int availableBytes = inputStream.available();
            if (availableBytes > 0) {
                byte[] readBytes = inputStream.readNBytes(availableBytes);
                String s = new String(readBytes, StandardCharsets.UTF_8);
                String[] split = s.trim().split("\r");
                if (split[0].startsWith("DATE =")) {
                    String name = split[2].split(" = ")[1];
                    String number = split[3].split(" = ")[1];
                    StringBuilder sb = new StringBuilder();
                    String title = "Incoming Call";
                    // sometimes name and number are the same, therefore when it is, don't repeat number
                    if(name.contains(number)) {
                        sb.append(name).append('\n');
                    } else {
                        sb.append(name).append(' ').append(number).append('\n');
                        title += " From " + name;
                    }
                    LocalDateTime localDateTime = LocalDateTime.now();
                    sb.append("on ").append(formatter.format(localDateTime));
                    // sends the caller ID data to Firebase
                    LandLineAlertServer.getInstance().sendMessage(title, sb.toString());
                }
            }
            // waits 1 second to reduce checking the serial port
            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, FirebaseMessagingException {
        LandLineAlert modem = new LandLineAlert();
        modem.run();
    }
}
