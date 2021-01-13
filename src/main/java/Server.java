import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server {
    @SuppressWarnings("unchecked")
    public static class MyThread extends Thread {
        Socket s;

        ArrayList<String> opened;
        ServerSocket ss;

        public MyThread(ServerSocket ss, Socket s, ArrayList<String> opened) {
            this.s = s;
            this.ss = ss;

            this.opened = opened;
        }

        @Override
        public void run() {
            //Reader для чтения сообщений
            BufferedReader bf = null;
            try {
                bf = new BufferedReader(new InputStreamReader(s.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String str = null;


            while (!s.isClosed()) {
                //читаем сообщение и если клиент закончил работу - выход из цикла
                // и конец работы потока
                try {
                    if ((str = bf.readLine()).equals("clientFinishedToWrite")) {
                        System.out.println("client disconnected");
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //PrintWriter для записи
                PrintWriter pr = null;
                try {
                    pr = new PrintWriter(s.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //если команда "finishServer" - закрываем ServerSocket
                if (str.equals("finishServer")) {
                    try {
                        ss.close();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // "gt $id" - отправляем клиенту все task'и пользователя $id
                else if (str.charAt(0) == 'g' && str.charAt(1) == 't') {
                    try {
                        String[] newStr = str.split(" ");
                        JSONParser parser = new JSONParser();
                        Object obj = parser.parse(new FileReader("data.json"));
                        JSONObject jsonObjectbj = (JSONObject) obj;
                        JSONObject jsonId = (JSONObject) jsonObjectbj.get(newStr[1]);
                        String name = (String) jsonId.get("name");
                        String surname = (String) jsonId.get("surname");
                        String listOfTasks = ((JSONObject) jsonId.get("listOfTasks")).toJSONString();
                        pr.println("name: " + name + ", surname: " + surname + ", tasks: " + listOfTasks);
                    } catch (Exception e) {
                        System.out.println("wrong format gt id");
                    }
                }
                // "a $id $taskName $start $finish" -
                // добавляем пользователю $id $task с указанным временем
                else if (str.charAt(0) == 'a') {
                    pr.println("");
                    try {
                        String[] newStr = str.split(" ");
                        JSONParser parser = new JSONParser();
                        Object obj = parser.parse(new FileReader("data.json"));
                        JSONObject jsonObjectbj = (JSONObject) obj;
                        JSONObject jsonId = (JSONObject) jsonObjectbj.get(newStr[1]);
                        JSONObject listOfTasks = (JSONObject) jsonId.get("listOfTasks");
                        JSONObject task = new JSONObject();
                        listOfTasks.put(newStr[2], task);
                        task.put("start", newStr[3]);
                        task.put("finish", newStr[4]);

                        FileWriter f = new FileWriter("data.json");
                        f.write(obj.toString());
                        f.flush();
                    } catch (Exception e) {
                        System.out.println("wrong format a id taskName time1 time2");
                    }
                }
                // "r $id $taskName" - удаляем $task пользователя $id
                else if (str.charAt(0) == 'r') {
                    pr.println("");
                    try {
                        String[] newStr = str.split(" ");
                        JSONParser parser = new JSONParser();
                        Object obj = parser.parse(new FileReader("data.json"));
                        JSONObject jsonObjectbj = (JSONObject) obj;
                        JSONObject jsonId = (JSONObject) jsonObjectbj.get(newStr[1]);
                        JSONObject listOfTasks = (JSONObject) jsonId.get("listOfTasks");
                        listOfTasks.remove(newStr[2]);

                        FileWriter f = new FileWriter("data.json");
                        f.write(obj.toString());
                        f.flush();
                    } catch (Exception e) {
                        System.out.println("wrong format r id taskName");
                    }
                }
                // "с $id $taskName $start $finish" - меняем указанный
                // $taskName;
                // сделан sleep на 10 секунд для правдоподобности
                // когда клиент делает данный запрос - элемент, который
                // он намерен поменять - записывается в opened;
                // если другой клиент попытается в это время изменить этот же
                // элемент - он ставится в очередь (ему отправляется
                // "wait", и он раз в секунду пытается вновь получить доступ.
                // Он его получит только тогда, когда первый клиент закончит
                // работу с элементом, и символическое название элемента -
                // удалится из opened.
                else if (str.charAt(0) == 'c') {

                    try {
                        String str012 = str.split(" ")[0] +
                                str.split(" ")[1] +
                                str.split(" ")[2];
                        if (!opened.contains(str012)) {
                            pr.println("");
                            opened.add(str012);
                               /* long original = System.currentTimeMillis();
                                while (true) {
                                    if (System.currentTimeMillis() - original >= 10000) {
                                        break;
                                    }
                                }*/
                            sleep(10000);
                            String[] newStr = str.split(" ");
                            JSONParser parser = new JSONParser();
                            Object obj = parser.parse(new FileReader("data.json"));
                            JSONObject jsonObjectbj = (JSONObject) obj;
                            JSONObject jsonId = (JSONObject) jsonObjectbj.get(newStr[1]);
                            JSONObject listOfTasks = (JSONObject) jsonId.get("listOfTasks");
                            listOfTasks.remove(newStr[2]);

                            JSONObject task = new JSONObject();
                            listOfTasks.put(newStr[2], task);
                            task.put("start", newStr[3]);
                            task.put("finish", newStr[4]);

                            FileWriter f = new FileWriter("data.json");
                            f.write(obj.toString());
                            f.flush();
                            opened.remove(str012);

                        } else {
                            sleep(1000);
                            pr.println("wait");
                        }
                    } catch (Exception e) {
                        System.out.println("wrong format c id taskName time1 time2");

                    }
                } else pr.println("");

                pr.flush();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(4999);
        System.out.println("server is running on port " + ss.getLocalPort());
        //ячейки, к которым нельзя обратиться в данный момент
        ArrayList<String> opened = new ArrayList();

        //json();

        //создаём сокет и при присоединении клиента - создаём для него поток
        while (true) {
            Socket s = null;
            try {
                s = ss.accept();
            } catch (Exception e) {
                break;
            }
            System.out.println("accepted");

            Thread thread = new MyThread(ss, s, opened);
            thread.start();
        }
    }

    @SuppressWarnings("unchecked")
    static void json() throws IOException {
        JSONObject all = new JSONObject();

        JSONObject person1 = new JSONObject();
        all.put("id_1", person1);
        JSONObject person2 = new JSONObject();
        all.put("id_2", person2);
        JSONObject person3 = new JSONObject();
        all.put("id_3", person3);
        JSONObject person4 = new JSONObject();
        all.put("id_4", person4);

        JSONObject listOfTasks1 = new JSONObject();
        person1.put("listOfTasks", listOfTasks1);
        person1.put("name", "Adam");
        person1.put("surname", "Adamov");
        JSONObject listOfTasks2 = new JSONObject();
        person2.put("listOfTasks", listOfTasks2);
        person2.put("name", "Boris");
        person2.put("surname", "Borisov");
        JSONObject listOfTasks3 = new JSONObject();
        person3.put("listOfTasks", listOfTasks3);
        person3.put("name", "Charlie");
        person3.put("surname", "Charliev");
        JSONObject listOfTasks4 = new JSONObject();
        person4.put("listOfTasks", listOfTasks4);
        person4.put("name", "Dima");
        person4.put("surname", "Dimin");

        JSONObject task1_1 = new JSONObject();
        listOfTasks1.put("task1_1", task1_1);
        JSONObject task1_2 = new JSONObject();
        listOfTasks1.put("task1_2", task1_2);

        JSONObject task2_1 = new JSONObject();
        listOfTasks2.put("task2_1", task2_1);
        JSONObject task2_2 = new JSONObject();
        listOfTasks2.put("task2_2", task2_2);

        JSONObject task3_1 = new JSONObject();
        listOfTasks3.put("task3_1", task3_1);
        JSONObject task3_2 = new JSONObject();
        listOfTasks3.put("task3_2", task3_2);

        JSONObject task4_1 = new JSONObject();
        listOfTasks4.put("task4_1", task4_1);
        JSONObject task4_2 = new JSONObject();
        listOfTasks4.put("task4_2", task4_2);

        task1_1.put("start", "00-00");
        task1_1.put("finish", "00-03");

        task1_2.put("start", "00-00");
        task1_2.put("finish", "00-03");

        task2_1.put("start", "00-00");
        task2_1.put("finish", "00-03");

        task2_2.put("start", "00-00");
        task2_2.put("finish", "00-03");

        task3_1.put("start", "00-00");
        task3_1.put("finish", "00-03");

        task3_2.put("start", "00-00");
        task3_2.put("finish", "00-03");

        task4_1.put("start", "00-00");
        task4_1.put("finish", "00-03");

        task4_2.put("start", "00-00");
        task4_2.put("finish", "00-03");

        FileWriter f = new FileWriter("data.json");
        f.write(all.toString());

        f.flush();
    }
}