import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public Button send;
    public ListView<String> listView;
    public TextField text;
    private List<File> clientFileList;
    public static Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    public final static String clientPath = "./client/src/main/resources";

    public void sendCommand(ActionEvent actionEvent) {
        String msg = text.getCharacters().toString();

        // download message
        if(msg.startsWith("./download")) {
            try {
                String fileName = msg.split(" ")[1];
                System.out.println("Downloading file " + fileName);
                os.writeUTF("./download");
                os.writeUTF(fileName);
                os.flush();
                String response = is.readUTF();
                System.out.println(response);
                if(response.equals("OK")) {
                    long fileLength = is.readLong();
                    File file = new File(clientPath + "/" + fileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        byte [] buffer = new byte[1024];
                        for (long i = 0; i < (fileLength / 1024 == 0 ? 1 : fileLength / 1024); i++) {
                            int bytesRead = is.read(buffer);
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("SEND!");
    }


    private void init() {
        try {
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init();
        try{
            clientFileList = new ArrayList<>();
            File dir = new File(clientPath);
            if (!dir.exists()) {
                throw new RuntimeException("directory resource not exists on client");
            }
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                clientFileList.add(file);
                listView.getItems().add(file.getName() + " : " + file.length());
            }
            listView.setOnMouseClicked(a -> {
                if (a.getClickCount() == 2) {
                    String fileName = getFilenameFromRecord(listView.getSelectionModel().getSelectedItem());
                    File currentFile = findFileByName(fileName);
                    if (currentFile != null) {
                        try {
                            System.out.println("Uploading file " + fileName);
                            os.writeUTF("./upload");
                            os.writeUTF(fileName);
                            os.writeLong(currentFile.length());
                            FileInputStream fis = new FileInputStream(currentFile);
                            byte [] buffer = new byte[1024];
                            while (fis.available() > 0) {
                                int bytesRead = fis.read(buffer);
                                os.write(buffer, 0, bytesRead);
                            }
                            os.flush();
                            String response = is.readUTF();
                            System.out.println(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFilenameFromRecord(String record) {
        return record.split(":")[0].trim();
    }

    private File findFileByName(String fileName) {
        for (File file : clientFileList) {
            if (file.getName().equals(fileName)){
                return file;
            }
        }
        return null;
    }
}
