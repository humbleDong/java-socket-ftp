package com.ldd.server;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * TcpHandler
 */
public class TcpHandler implements Runnable{
    private DatagramSocket udpSocket;
    private Socket socket;
    // 当前root路径
    private String root;
    // 初始root
    private static String ROOT;
    // 当前root文件
    private File rootFile;
    // 指令集
    private static final String[] CMD_BAG = {"ls", "bye", "help", "cd", "get"};
    // 指令集描述
    private static final String[] CMD_DES = {
              "[1]\tls\t服务器返回当前目录文件列表（<file/dir>\tname\tsize）"
            , "[2]\tbye\t断开连接，客户端运行完毕"
            , "[3]\thelp\t获取指令集信息"
            , "[4]\tcd <dir>\t进入指定目录"
            , "[5]\tget <file>\t通过UDP下载指定文件，保存到客户端当前目录下"
    };

    // 常见图片格式
    private static final String[] IMAGE_SET = {
            "PG", "jpg","tiff","bmp","BMP","gif","GIF",
            "WBMP","png","PNG","JPEG","tif","TIF","TIFF","wbmp", "jpeg"
    };

    public TcpHandler(Socket socket ,DatagramSocket udpSocket, String root) {
        this.socket = socket;
        this.udpSocket = udpSocket;
        this.root = root;
        this.ROOT = root;
        this.rootFile = new File(root);
    }
    @Override
    public void run() {
        try {
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            DataOutputStream dos = new DataOutputStream(os);
            DataInputStream dis = new DataInputStream(is);

            dos.writeUTF("connected");
            dos.flush();

            // 循环标记
            boolean flag = true;
            while(flag) {
                try {
                    String msg = dis.readUTF();
                    System.out.println("客户端" + socket.getInetAddress().getHostAddress()
                            + ":客户端" + socket.getPort() + ">请求操作：" + msg);
                    // 判断客户端输入信息
                    flag = processCmd(dos, dis, msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    closeSocket(dos, dis);
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 处理指令
     * @param dos
     * @param dis
     * @param msg 输入信息
     * @return boolean 循环标记
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean processCmd(DataOutputStream dos, DataInputStream dis, String msg) throws IOException, InterruptedException {
        if(isLegalCmd(msg)) {//指令合法
            if(msg.equals("ls")) {
                lsFun(dos);
            } else if(msg.startsWith("cd")) {
                cdFun(dos, msg);
            } else if(msg.startsWith("get")) {
                getFun(dos, msg);
            } else if(msg.equals("bye")) {
                closeSocket(dos, dis);
                // 返回循环标记为false
                return false;
            } else if(msg.equals("help")) {
                helpFun(dos);
            }
        } else {
            unknownCmd(dos);
        }
        return true;
    }

    /**
     * 判断 cmd是否合法
     * @param msg 输入信息
     * @return boolean
     */
    public boolean isLegalCmd(String msg) {
        // 合法标记
        boolean flag = false;
        for (int i = 0; i < CMD_BAG.length; i++) {
            if (i < 3) {
                if (CMD_BAG[i].equals(msg)) {
                    flag = true;
                }
            } else if (i == 3){// cd
                if (msg.startsWith(CMD_BAG[i])) {
                    // 如果输入为cd..或cd xx的形式，cmd合法（xx是否合法在cdFun中判断）
                    if ("cd..".equals(msg) ||
                            (msg.split(" ").length == 2 &&
                                    msg.split(" ")[0].equals("cd"))) {
                        flag = true;
                    }
                }
            } else if (i == 4) {// get
                if (msg.startsWith(CMD_BAG[i])) {
                    // 如果输入为get xx的形式，cmd合法（xx是否合法在getFun中判断）
                    if (msg.split(" ").length == 2 &&
                            msg.split(" ")[0].equals("get")) {
                        flag = true;
                    }
                }
            }
        }
        return flag;
    }

    /**
     * 客户端输入ls指令时，执行该方法
     * @param dos 数据输出流
     * @throws IOException
     */
    public void lsFun(DataOutputStream dos) throws IOException {
        String listMsg = "";
        // 当前root文件的子文件
        Iterator<File> iterator = Arrays.stream(rootFile.listFiles()).iterator();
        listMsg += String.format("%-10s %-30s %3s", "fileType", "fileName", "fileSize");
        listMsg += "\n";
        while(iterator.hasNext()) {
            File file = iterator.next();
            // 获取文件类型、名称、大小
            String fileType = file.isDirectory() ? "<dir>" : "<file>";
            String fileName = file.getName();
            long fileSize = getFileSize(file);
            //排版用
            listMsg += String.format("%-10s %-30s %d", fileType, fileName, fileSize);
            // 判断是否是最后一个
            if(iterator.hasNext()) {
               listMsg += "\n";
            }

        }
        dos.writeUTF(listMsg);
        dos.flush();
    }

    /**
     * 客户端输入cd指令时调用
     * @param dos
     * @param msg 输入字符串
     * @throws IOException
     */
    public void cdFun(DataOutputStream dos, String msg) throws IOException {
        // 输入为cd..的情况
        if("cd..".equals(msg)) {
            // 判断是否是根目录
            if(!root.equals(ROOT)) {
                root = rootFile.getParent();
                rootFile = rootFile.getParentFile();
            }
            dos.writeUTF("curseEnv > OK");
            dos.flush();
        } else { // 输入为cd xx的情况
            String nextRoot = msg.split(" ")[1];
            // 判断xx是否合法
            if(hasFile(rootFile.listFiles(), nextRoot)) {
                String newRoot = root + "\\" + nextRoot;
                File newRootFile = new File(newRoot);
                if(newRootFile.isDirectory()) {
                    root = newRoot;
                    rootFile = newRootFile;
                    dos.writeUTF(nextRoot + " > OK");
                    dos.flush();
                    return;
                }
            }
            dos.writeUTF("unknown dir");
            dos.flush();
        }
    }

    /**
     * 客户端输入get指令时执行
     * @param dos
     * @param msg
     * @throws IOException
     */
    public void getFun(DataOutputStream dos, String msg) throws IOException, InterruptedException {
        // 判断xx文件是否合法
        if(!hasFile(rootFile.listFiles(), msg.split(" ")[1])) {
            dos.writeUTF("unknown file");
            dos.flush();
            return;
        }
        DatagramSocket datagramSocket = udpSocket;
        String filePath = root + "\\" + msg.split(" ")[1];
        File file = new File(filePath);

//        FileReader fileReader = null;
//        fileReader = new FileReader(file);
//        BufferedReader reader = new BufferedReader(fileReader);

        // 字节流
        FileInputStream fis = new FileInputStream(file);
        // 开始读取提示信息
        dos.writeUTF("reading start 文件物理路径：" + file.getAbsolutePath() + " 文件大小：" + file.length());
        dos.flush();
        // 获取客户端UDP port
        byte[] udpPortByte = new byte[1024];
        DatagramPacket inPacket = new DatagramPacket(udpPortByte, udpPortByte.length);
        datagramSocket.receive(inPacket);
        int clientUdpPort = Integer.parseInt((new String(inPacket.getData(),
                0 ,inPacket.getLength())));

//        String readContent;
//        while((readContent = reader.readLine())!= null) {
//            byte[] bytes = (readContent + "\n").getBytes();
//            int len = bytes.length;
//            DatagramPacket packet = new DatagramPacket(bytes, len,
//                    socket.getInetAddress().getLocalHost(), clientUdpPort);
//            datagramSocket.send(packet);
//            dos.writeUTF("reading");
//            dos.flush();
//            TimeUnit.MICROSECONDS.sleep(1);
//        }
        // 不断读取文件并写入DatagramPacket
        int len = 0;
        byte buffer[] = new byte[1024];
        while ((len = fis.read(buffer)) != -1) {
            DatagramPacket packet = new DatagramPacket(buffer, len,
                    socket.getInetAddress().getLocalHost(), clientUdpPort);
            datagramSocket.send(packet);
            dos.writeUTF("reading");
            dos.flush();
            TimeUnit.MICROSECONDS.sleep(1);
        }
        // 读取结束关闭资源
//        reader.close();
        fis.close();
        dos.writeUTF("reading end");
        dos.flush();
    }

    /**
     * 关闭客服端 socket
     * @param dos
     * @param dis
     * @throws IOException
     */
    public void closeSocket(DataOutputStream dos, DataInputStream dis) throws IOException {
        System.out.println("客户端" + socket.getInetAddress().getHostAddress()
                + ":客户端" + socket.getPort() + ">已离线！");
        dos.writeUTF("bye");
        dos.flush();
        dis.close();
        dos.close();
        socket.close();
    }

    /**
     * 获取指令集描述
     * @param dos
     * @throws IOException
     */
    public void helpFun(DataOutputStream dos) throws IOException {
        String des = "";
        Iterator<String> iterator = Arrays.stream(CMD_DES).iterator();
        while(iterator.hasNext()) {
            des += iterator.next();
            // 如果是最后一条则不换行
            if(!iterator.hasNext()) {
                break;
            }
            des += "\n";
        }
        dos.writeUTF(des);
        dos.flush();
    }

    /**
     * 客户端输入未知指令时调用
     * @param dos
     * @throws IOException
     */
    public void unknownCmd(DataOutputStream dos) throws IOException {
        dos.writeUTF("unknown cmd");
        dos.flush();
    }

    /**
     * 获取目录或文件大小
     * @param file
     * @return
     */
    public long getFileSize(File file) {
        long size = 0;  //文件大小

        if (file.isFile())  //如果是文件
            size += file.length();
        else {  //如果是目录
            File[] files = file.listFiles();    //目录获取文件数组
            for (int i = 0; files != null && i < files.length; i++)
                size += getFileSize(files[i]);  //递归调用本方法
        }
        return size;
    }

    /**
     * 判断 files里是否存在名为 fileName的文件
     * @param files
     * @param fileName
     * @return boolean
     */
    public boolean hasFile(File[] files, String fileName) {
        boolean res = false;
        for (File file : files) {
            if(file.getName().equals(fileName)) res = true;
        }
        return res;
    }
}
