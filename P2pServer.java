package p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.JFrame;

public class P2pServer extends JFrame {
	final int server_port = 7777;
	Socket client;
	Vector v_client_list;
	PrintWriter requestor;

	public P2pServer() {
		// client 수 관리 vector
		v_client_list = new Vector();

		// client가 network 상태인지 확인하기 위해 일정 간격으로 신호를 보내기 위한 timer 생성
		Timer timer = new Timer();

		// 2초마다 Check_client 객체 생성
		timer.schedule(new Check_client(), 0, 2 * 1000);

		try {
			// 포트번호 12167에 SocketServer생성
			ServerSocket server_socket = new ServerSocket(server_port);
			while (true) {
				try {
					// 접속할 client를 관리할 Socket 객체 생성
					Socket server = server_socket.accept();

					// client가 독립적으로 io작업을 할 수 있도록 Connection class 생성
					Connection c = new Connection(server, this);
					// 새로 접속한 client 를 client목록에 추가
					addClient(c);
					// Connection class 가 가지고 있는 run메서드를 실행시켜 client와의 통신을 유지
					c.start();
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {
			System.err.println("sever : " + e);
		}
	}

	public void message(String msg) {
		// client 목록을 한 바퀴 돌면서 msg보냄
		for (int i = 0; i < v_client_list.size(); i++) {
			((Connection) v_client_list.elementAt(i)).sendMessage(msg);

		}
	}

	// addClient 메소드 : 새로운 client 추가
	public void addClient(Connection c) {
		v_client_list.addElement(c);
	}

	// setRequestor 메소드 : client가 보낸 메시지에 대해 바로 그 client에만 보내기 위한 outputSteram
	public synchronized void setRequestor(PrintWriter requestor) {
		// 검색을 요청한 client에게만 검색 결과를 보내기 위해
		this.requestor = requestor;
	}

	// removeClient 메소드 : network가 끊긴 client 삭제
	public void removeClient(Connection c) {
		// Check_client class가 client와 통신을 시도하다 실패하면 해당 socket을 닫는다.
		c.closeSocket();

		// client 목록에서 삭제
		boolean b = v_client_list.remove(c);
	}

	class Connection extends Thread {
		// client와 통신을 위해 만들어진 socket 이것에서 io를 뽑아낸다.
		Socket socket;
		// P2pServer의 class의 메서드를 사용하기 위해
		P2pServer p2p_server;

		// client에 들어온 메시지를 받기위해
		BufferedReader in;

		// client에게 메시지를 보내기위해
		PrintWriter out;

		// Connection 생성자
		public Connection(Socket s, P2pServer j) {
			socket = s;
			p2p_server = j;

			try {
				// inputStream 생성
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				// outputStream 생성
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (Exception e) {

			}
		}

		public void run() {
			String msg = "";
			// client로부터 메시지가 들어오기를 계속 대기
			while (true) {
				try {
					// client로부터 메시지 한 줄 받기
					msg = in.readLine();
					System.out.println(msg);
					if (msg != null) { // 메시지가 null이 아닌 경우
						if (msg.startsWith("#c#")) {
							// client로부터의 메시지가 #c#으로 시작되는 경우, 즉 network상태인가 알아보는 신호

							// socket을 통해 InetAddress class 를 생성시킨다. 다음 줄에서 ip address를 뽑기 위해
							InetAddress client_ip = socket.getInetAddress();

							// 윗줄에서 얻어진 InetAddress class의 getHostAddress 메서드로 해당 client ip address를 뽑아낸다
							String s_client_ip = client_ip.getHostAddress();

							System.out.println("client(" + s_client_ip + ") is up and running");
						} else if (msg.startsWith("#s#")) {
							// client로부터의 메시지가 #s#으로 시작되는 경우, 즉 검색하고자 하는 메시지를 받는 경우
							// P2pServer class의 message메서드를 통해 모든 client에게 메시지 전송
							p2p_server.message(msg);
							// #s#lkaskdfj <- 이런식으로 넘어옴
							// P2pServer class에서 검색결과를 요청한 client에게만 돌려줄 outstream 설정
							p2p_server.setRequestor(out);
						} else if (msg.startsWith("#f#")) {
							// 넘어온 msg의 형태가 #f#README.html@127.0.0.1:::#f#README.txt@127.0.0.1
							// 각 client로부터의 들어온 #f#시작된 메시지의 검색을 요청한 client에게 보여준다.
							// requestor는 PrintWriter 객체로 위 #s#의 경우에서 p2p_server.setRequestor(out);로 설정되었다.
							requestor.println("#r#" + msg);
						}
					} else {
						break;
					}
				} catch (Exception e) {
					p2p_server.removeClient(this);
				} // try-catch
			} // while
		}// run

		public void sendMessage(String msg) {
			try {
				out.println(msg);
				// 자신의 client에게 메시지 보내기
			} catch (Exception e) {

			} // try-catch
		}// sendMessage

		// closeSocket
		public void closeSocket() { // 사용된 io와 socket 닫기
			try {
				in.close();
				out.close();
				socket.close();
			} catch (Exception e) {

			} // try-catch
		}
	} // end Connection Class

	class Check_client extends TimerTask { // TimerTask에서 상속 받으면 run()메서드 override해야됨
		public void run() {
			int client_size = v_client_list.size();

			System.out.println("*****************************");
			System.out.println("**      상태 check         **");
			System.out.println("*****************************");
			System.out.println("**    # of clients : " + client_size + "     **");
			System.out.println("*****************************");

			for (int i = 0; i < client_size; i++) {
				try {
					((Connection) v_client_list.elementAt(i)).sendMessage("#c#" + client_size);
					// client목를 쭈욱 한 바퀴 돌면서 check 메시지 보낸다(#c#)
				} catch (Exception e) {
					v_client_list.removeElementAt(i);
					// 통신이 되지 않을 경우 목록에서 삭제
				} // try-catch
			} // for
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
		} // run
	} // Check_client Class

	public static void main(String[] args) {
		new P2pServer();
	}

}
