package ru.barbarossa;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Greshilov
 */
public class RemoteControl implements Runnable {

    private Session session;
    private RemoteStation target;
    private File configTemplate;
    private String port;
    private String[] allowedIP;
    private boolean useUsernameAuth;

    public RemoteControl(RemoteStation connection) {
        this.target = connection;
    }
    
    /**
     * Метод запускающий выполнение сценария на указанной машине для запуска
     * тампрокси сервера
     *
     * @param connection
     * @param templateConfig
     * @param allowedAddreses
     * @param port порт разворачивания прокси сервера
     */
    public RemoteControl(RemoteStation connection, File templateConfig, String port, String[] allowedAddreses) {
        this.target = connection;
        this.configTemplate = templateConfig;
        this.allowedIP = allowedAddreses;
        this.port = port;
    }
    
    /**
     * Метод запускающий выполнение сценария на указанной машине для запуска
     * там прокси сервера
     */
    @Override
    public void run() {
        String remoteFileName = "/tmp/" + target.getHostname().replaceAll("\\.", "");

        MainFrame.out("Начало работы с " + target.getHostname());
        if (connect()) {
            if (createInterfacesFile(remoteFileName)) {
                //Получить удаленный файл
                String[] arrayIP = getInterfaces(remoteFileName);
                if (arrayIP.length != 0 && arrayIP != null) {
                    MainFrame.out("\n" + target.getHostname() + " - Сборка Dante 1.4.1 из исходников. (Это может занять некоторое время)");
                    installProxy();
                    sendInitScript(new File("script.sh"));
                    MainFrame.out("\n" + target.getHostname() + " - Dante 1.4.1 собран и устрановлен");
                    if (configureProxy(arrayIP)) {
                        restartProxy();
                        startProxy();
                        result(arrayIP);
                    } else {
                        MainFrame.out("\n" + target.getHostname() + " - Ошибка при настройке");
                    }
                } else {
                    MainFrame.out("\n" + target.getHostname() + " - Ошибка при получении интерфейсов с");
                }
            } else {
                MainFrame.out("\n" + target.getHostname() + " - Ошибка при создании файла интерфейсов на");
            }
        } else {
            MainFrame.out("\n" + target.getHostname() + " - Ошибка при подключении");
        }
    }

    /**
     * Соедениться с сервером через ssh
     *
     * @return успешно или нет
     */
    protected Boolean connect() {
        try {
            JSch jsch = new JSch();
            setSession(jsch.getSession(getTarget().getUsername(),
                    getTarget().getHostname(), 22));
            getSession().setPassword(getTarget().getPassword());
            getSession().setConfig("StrictHostKeyChecking", "no");
            getSession().connect();
            return true;
        } catch (JSchException e) {
            //System.outAreaStream.println(e);
            System.out.println(e);
            return false;
        }
    }

    /**
     * Создать файл список-интерфейсов удаленного сервера
     *
     * @param remoteFileName
     * @return
     */
    protected boolean createInterfacesFile(String remoteFileName) {
        String myScript
                = "rm " + remoteFileName + "\n"
                + "touch " + remoteFileName + "\n"
                + "ip addr show | sed -n '/inet /{s/^.*inet \\([0-9.]\\+\\).*$/\\1/;p}' >> " + remoteFileName + "\n";
        //Создать файл со списоком интерфейсов
        return executeCommand(getSession(), myScript);
    }

    /**
     * Функция получения списка интерфейсов удаленного сервера
     *
     * @param pathToRemoteFile путь к файлу на удаленной машине
     * @return Список интерфейсов или null при неудаче
     */
    public String[] getInterfaces(String pathToRemoteFile) {
        File newFile;
        try {
            String command = "scp -f " + pathToRemoteFile;
            Channel channel = this.session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] buf = new byte[1024];

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();

            while (true) {
                int c = checkAck(in);
                if (c != 'C') {
                    break;
                }
                // read '0644 '
                in.read(buf, 0, 5);

                long filesize = 0L;
                while (true) {
                    if (in.read(buf, 0, 1) < 0) {
                        return null;
                    }
                    if (buf[0] == ' ') {
                        break;
                    }
                    filesize = filesize * 10L + (long) (buf[0] - '0');
                }

                String file = null;
                for (int i = 0;; i++) {
                    in.read(buf, i, 1);
                    if (buf[i] == (byte) 0x0a) {
                        file = new String(buf, 0, i);
                        break;
                    }
                }

                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();
                //newFile = new File(destFolder + target.getHostname());
                newFile = new File(target.getHostname().replaceAll("\\.", "") + ".txt");
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int foo;
                    while (true) {
                        if (buf.length < filesize) {
                            foo = buf.length;
                        } else {
                            foo = (int) filesize;
                        }
                        foo = in.read(buf, 0, foo);
                        if (foo < 0) {
                            return null;
                        }
                        fos.write(buf, 0, foo);
                        filesize -= foo;
                        if (filesize == 0L) {
                            break;
                        }
                    }
                }
                if (checkAck(in) != 0) {
                    System.exit(0);
                }
                // send '\0'
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();
                List<String> list = new ArrayList<>();
                try (Scanner inBuf = new Scanner(newFile)) {
                    while (inBuf.hasNextLine()) {
                        String curElem = inBuf.nextLine();
                        if (!curElem.contains("127.0.0.1")) {
                            list.add(curElem);
                        }
                    }
                }
                newFile.delete();
                return list.toArray(new String[list.size()]);
            }
        } catch (JSchException | IOException e) {
            System.out.println(e);
        }
        return null;
    }

    /**
     * Создает скрипт, инициирующий работу прокси сервера
     *
     * @param initScript скрипт нужный для запуска приложения
     * @return успешно ли выполнена команда
     */
    protected boolean sendInitScript(File initScript) {
        String myScript;
        InputStream fis;
        BufferedReader br;
        String line;
        try {
            fis = new FileInputStream(initScript);
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            String configName = "/etc/init.d/sockd";

            myScript
                    = "rm -rf /etc/init.d/sockd\n"
                    + "touch /etc/init.d/sockd\n"
                    + "chmod 100 /etc/init.d/sockd\n";

            while ((line = br.readLine()) != null) {
                myScript += "echo \t\'" + line + "\'\t >> " + configName + "\n";
            }
            // Done with the file
            br.close();
            return executeCommand(getSession(), myScript);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Сборка и установка последней версии прокси сервера из исходников
     *
     * @return успешкно ли прошла установка
     */
    protected boolean installProxy() {
        String myScript
                = "apt-get update\n"
                + "apt-get install make gcc -y\n"
                + "cd /var\n"
                + "wget https://www.inet.no/dante/files/dante-1.4.1.tar.gz --no-check-certificate\n"
                + "tar xvfz dante-*\n"
                + "cd dante-*\n"
                + "./configure\n"
                + "make\n"
                + "make install\n";
        //Сборка и установка последней версии прокси сервера из исходников
        return executeCommand(getSession(), myScript);
    }

    /**
     * Метод передачи настроек на удаленный сервер
     *
     * @param configTemplate
     * @param arrayIP
     * @param allowedIP
     * @return успешно или нет настроен прокси
     */
    private boolean configureProxy(String[] interfaces) {
        String myScript;
        InputStream fis;
        BufferedReader br;
        String line;
        try {
            fis = new FileInputStream(configTemplate);
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));

            String configName = "/etc/sockd.conf";
            myScript
                    = "rm  " + configName + "\n"
                    + "touch " + configName + "\n";

            while ((line = br.readLine()) != null) {
                switch (line) {
                    case "<internal-settings>":
                        for (String ip : interfaces) {
                            myScript
                                    += "echo internal: " + ip + " port = "+port
                                    + " >> " + configName + "\n";
                        }
                        break;
                    case "<external-settings>":
                        for (String ip : interfaces) {
                            myScript
                                    += "echo external: " + ip
                                    + " >> " + configName + "\n";

                        }
                        break;
                    case "<allowed-ip>":
                        for (String IP : allowedIP) {
                            myScript
                                    += "echo \'client pass { from: " + IP + "/0 port 1-65535 to: 0.0.0.0/0 }\' >> " + configName + "\n";
                        }
                        break;
                    default:
                        myScript += "echo " + line + " >> " + configName + "\n";
                        break;
                }
            }
            // Done with the file
            br.close();
            return executeCommand(getSession(), myScript);
        } catch (IOException ex) {
        }
        return false;
    }

    protected void startProxy() {
        executeCommand(getSession(), "/etc/init.d/sockd start\n");
    }

    protected void restartProxy() {
        executeCommand(getSession(), "/etc/init.d/sockd restart\n");
    }

    protected void result(String[] arrayIP) {
        try {
            String s = "";
            for (String itemIP : arrayIP) {
                s += itemIP + ":" + getPort() + "\n";
            }

            MainFrame.out("Прокси-сервер на " + target.getHostname()
                    + " сконфигурирован и запущен. Доступ по адресам:"
                    + "\n" + s);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Метод для выполнения последовательности команд через ssh сессию
     *
     * @param session сессия работы через ssh
     * @param command команда
     * @return Успешно ли выполнена команда
     */
    public boolean executeCommand(Session session, String command) {
        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);

            ((ChannelExec) channel).setErrStream(System.err);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                }
            }
            channel.disconnect();
            return channel.getExitStatus() == 0;

        } catch (JSchException | IOException e) {
            System.out.println(e);
            return false;
        }
    }

    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) {
            return b;
        }
        if (b == -1) {
            return b;
        }

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    public RemoteStation getTarget() {
        return target;
    }

    public File getConfigTemplate() {
        return configTemplate;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String[] getAllowedIP() {
        return allowedIP;
    }

    public void setAllowedIP(String[] allowedIP) {
        this.allowedIP = allowedIP;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
