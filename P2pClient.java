package p2p;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class P2pClient extends JFrame
		implements KeyListener, MouseListener, ActionListener, Runnable, ListSelectionListener {
	ServerSocket server_socket;
	Socket socket_to_host;
	String host_address = "127.0.0.1";
	// server와 통신할 port
	int port_to_host_number = 7777;
	// client끼리 통신할 port 
	static int p2p_port_number = 1001;

	Desktop desktop = Desktop.getDesktop();
	boolean serverConnect = false;
	boolean shareFolderSet = false;
	boolean downFolderSet = false;
	static JProgressBar downBar; // 다운로드 진행바
	JPopupMenu pMenu = new JPopupMenu("fileEdit");
	JMenuItem open;
	JMenuItem openFolder;
	JMenuItem deleteOnList;

	File file;
	File savefile;
	String pathname;
	String savepathname;
	String[] sa_file_list;
	String s_local_address;
	JTextField jtf_search;
	BufferedReader in = null;
	PrintWriter out;
	JList receiveList;
	JList downList;
	DefaultListModel receiveListModel;
	DefaultListModel downListModel;
	String download_filename;
	String downloadList_filename;

	static JLabel downFileName;
	JLabel serverState;

	JMenuBar menubar;
	JMenu menu;
	JMenuItem server_ip;
	JMenuItem directory;
	JMenuItem save_directory;

	JPanel panel_search; // 검색 Panel
	JPanel panel_download; // 다운로드 창
	JPanel panel_left; // 상단 창
	JPanel panel_fileList; // 검색된 파일리스트 창
	JPanel panel_state; // 상태
	static JPanel panel_temp;
	JSplitPane resultSplitPane = null;
	JSplitPane splitPane = null;
	JSplitPane statePane = null;
	JButton search; // 검색 버튼
	JButton download; // 다운로드 버튼
	JScrollPane resultPane; // 검색 결과
	JScrollPane downPane; // 다운로드 파일 목록
	JTextField jtf_server_ip;
	JFileChooser filechooser; // 파일 선택
	JFileChooser savefilechooser; // 저장파일 선택
	ImageIcon image_icon;

	JDialog dialog;
	JDialog downDialog;
	Thread t_connection;

	Color backBlack = new Color(0x4D4D4D);
	Color white = new Color(0xBABABA);

	public int ConnectCreation() {
		try {
			socket_to_host = new Socket(host_address, port_to_host_number);
			server_socket = new ServerSocket(p2p_port_number);
			getIpAddress();

			in = new BufferedReader(new InputStreamReader(socket_to_host.getInputStream()));
			out = new PrintWriter(socket_to_host.getOutputStream(), true);
			t_connection = new Thread(this);
			t_connection.start();
			P2p_server p2p_server = new P2p_server(server_socket);
			// p2p서버 클래스
		} catch (UnknownHostException e) {
			Alert("경고", "알수없는 호스트입니다.");
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			Alert("경고", "연결에 실패하였습니다.");
			return 0;
		} // try-catch
		return 1;
	}

	public P2pClient() {
		super("P2P");
		receiveListModel = new DefaultListModel();
		receiveList = new JList(receiveListModel);
		receiveList.setBackground(new Color(0xFCFCFC));
		receiveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 리스트 목록중 하나만 선택
		receiveList.addListSelectionListener(this); // list선택 바뀜
		receiveList.addMouseListener(this); // 더블클릭

		downListModel = new DefaultListModel();
		downList = new JList(downListModel);
		downList.setBackground(new Color(0xFCFCFC));
		downList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		downList.addListSelectionListener(new downListEvent());
		downList.addMouseListener(new downListEvent());
		menuSet();
		getContentPane().add(ui()).setBackground(Color.white);
		;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}

	public void getIpAddress() {
		InetAddress local_ip = socket_to_host.getLocalAddress();
		s_local_address = local_ip.toString();
		// 위에서 구하면 /127.0.0.1 처럼 나오는데, 앞의 슬래쉬(/) 를 제거하기 위한 코드 부분
		for (int i = s_local_address.length() - 1; i >= 0; i--) {
			if (s_local_address.charAt(i) == '/') {
				s_local_address = s_local_address.substring(i + 1);
				break;
			} // if
		} // for
	}

	public void menuSet() {
		menubar = new JMenuBar();
		menu = new JMenu("설정");
		server_ip = new JMenuItem("Server IP 설정");
		server_ip.addActionListener(this);
		server_ip.setActionCommand("server_ip");
		directory = new JMenuItem("공유 directory 설정");
		directory.addActionListener(this);
		directory.setActionCommand("directory");
		save_directory = new JMenuItem("저장 directory 설정");
		save_directory.addActionListener(this);
		save_directory.setActionCommand("save_directory");

		menu.add(server_ip);
		menu.add(directory);
		menu.add(save_directory);
		menubar.add(menu);

		open = new JMenuItem("열기");
		open.addActionListener(this);
		open.setActionCommand("openFile");
		openFolder = new JMenuItem("폴더 열기");
		openFolder.addActionListener(this);
		openFolder.setActionCommand("openFolder");
		deleteOnList = new JMenuItem("목록에서제거");
		deleteOnList.addActionListener(this);
		deleteOnList.setActionCommand("deleteOnList");

		pMenu.add(open);
		pMenu.add(openFolder);
		pMenu.add(deleteOnList);
		add(pMenu);
		setJMenuBar(menubar);
	}

	public Component ui() {
		panel_search = new JPanel(); // 검색창 (검색어, 검색 버튼)
		panel_download = new JPanel(); // 다운로드 (다운로드 버튼, 게이지)
		panel_fileList = new JPanel(); // 파일리스트 (검색된 파일리스트)
		panel_left = new JPanel(); // 좌측
		panel_state = new JPanel();
		panel_temp = new JPanel();
		// 검색창 text field 부분
		jtf_search = new JTextField(12);
		jtf_search.addKeyListener(this);

		// 검색 버튼 부분
		search = new JButton("Search");
		search.setPreferredSize(new Dimension(80, 24));
		search.setVerticalTextPosition(SwingConstants.CENTER);
		search.setFocusable(false);
		search.setBackground(Color.white);
		search.setForeground(backBlack);
		search.addActionListener(this);
		search.setActionCommand("search");

		// 다운로드 관련
		download = new JButton("Download");
		download.setVerticalTextPosition(SwingConstants.CENTER);
		download.setPreferredSize(new Dimension(90, 30));
		download.setFocusable(false);
		download.setBackground(Color.white);
		download.setForeground(backBlack);
		download.addActionListener(this);
		download.setActionCommand("download");

		downBar = new JProgressBar();
		downBar.setBackground(backBlack);
		downBar.setFont(new Font("Dialog", Font.BOLD, 12));
		downBar.setForeground(Color.white);
		downBar.setStringPainted(true);
		downBar.setPreferredSize(new Dimension(450, 30));
		downBar.setVisible(false);

		// 검색 부분, 다운로드 부분 panel 에 add
		panel_search.add(jtf_search);
		panel_search.add(search);

		downPane = new JScrollPane(downList);
		downFileName = new JLabel("not Selected");
		downFileName.setOpaque(true);
		downFileName.setHorizontalAlignment(JLabel.CENTER);
		downFileName.setPreferredSize(new Dimension(450, 30));
		downFileName.setBackground(backBlack);
		downFileName.setForeground(Color.white);
		downFileName.setFont(new Font("", Font.BOLD, 13));
		downFileName.setVisible(true);

		serverState = new JLabel("서버 연결 안됨");
		serverState.setForeground(Color.white);
		serverState.setHorizontalAlignment(JLabel.CENTER);
		JLabel llll = new JLabel("다운로드 목록");
		llll.setForeground(Color.white);
		llll.setHorizontalAlignment(JLabel.CENTER);
		panel_left.setLayout(new BorderLayout());
		panel_left.setPreferredSize(new Dimension(200, 400));
		panel_left.setBackground(backBlack);
		panel_left.add("South", serverState);
		panel_left.add("North", llll);
		panel_left.add(downPane);

		panel_state.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel_state.setBackground(new Color(0x181818));

		panel_state.add(jtf_search);
		panel_state.add(search);

		panel_temp.setBackground(new Color(0x181818));
		panel_temp.add(downFileName);
		panel_temp.add(downBar);
		panel_state.add(panel_temp);
		panel_state.add(download);

		resultPane = new JScrollPane(receiveList);
		resultPane.setSize(new Dimension(500, 400));
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel_left, resultPane);
		splitPane.setPreferredSize(new Dimension(800, 400));
		splitPane.setBackground(Color.black);
		resultSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel_state, splitPane);
		return resultSplitPane;
	}

	// 다른 client가 저장할 내 파일 directory
	public void directory_choose(String type) {
		filechooser = new JFileChooser();
		filechooser.setCurrentDirectory(new File("C:/"));
		filechooser.setFileSelectionMode(filechooser.DIRECTORIES_ONLY);

		int returnVal = filechooser.showOpenDialog(P2pClient.this); // 열기
		if (returnVal == filechooser.APPROVE_OPTION) {
			if (type == "Sharing") { // 공유
				file = filechooser.getSelectedFile();
				pathname = file.getAbsolutePath() + "/";
				System.out.println("directory : " + pathname);
				sa_file_list = file.list();
			} else if (type == "Save") { // 저장
				savefile = filechooser.getSelectedFile();
				savepathname = savefile.getAbsolutePath() + "/";
				System.out.println("save directory : " + savepathname);
			}
		}
	}

	// 저장 directory 선택
	public void save_directory_choose() {
		savefilechooser = new JFileChooser();
		savefilechooser.setCurrentDirectory(new File("C:/"));
		savefilechooser.setFileSelectionMode(savefilechooser.DIRECTORIES_ONLY);

		int returnVal1 = savefilechooser.showSaveDialog(P2pClient.this); // 저장
		if (returnVal1 == savefilechooser.APPROVE_OPTION) {
			savefile = savefilechooser.getSelectedFile();
			savepathname = file.getAbsolutePath() + "/"; // 파일의 절대경로 리턴 getAbsolutePath()
			System.out.println("save directory : " + savepathname); // 파일 저장 경로
		}
	}

	public String[] result_divide(String search_result) {
		String[] dividing = new String[2];
		for (int i = search_result.length() - 1; i >= 0; i--) {
			if (search_result.charAt(i) == '@') {
				dividing[1] = search_result.substring(i + 1);
				dividing[0] = search_result.substring(0, i);
			}
		}
		return dividing;
	}

	public void run() {
		String in_msg;
		try {
			while (true) {
				in_msg = in.readLine();
				System.out.println("넘어온 메시지" + in_msg);
				// 보통 #c#이 넘어오다가 검색하면 #s#sdfsdfwef 이 넘어옴
				// System.out.println(in_msg)
				if (in_msg != null) {
					// #s# 으로 시작하면.. 클라이언트가 검색한 내용이 서버를 통해서 브로드캐스트를 받아온 것이므로..
					if (in_msg.startsWith("#s#")) {
						String s_file_name_searched = in_msg.substring(3); // #s# 을 제외한 내용
						boolean isExist = false;
						String transfer_content = "";
						// sa_file_list <-- 설정된 디렉토리 내에 있는 파일들 및 디렉토리 receiveList;
						for (int i = 0; i < sa_file_list.length; i++) {
							// 디렉토리 빼고 파일명들만 추출
							if (new File(pathname + sa_file_list[i]).isFile()) {
								// 검색하려는 파일명의 일부라도 존재하면
								if (sa_file_list[i].toLowerCase().indexOf(s_file_name_searched.toLowerCase()) != -1) {
									System.out.println(pathname + sa_file_list[i]);
									isExist = true;
									// 전송할 내용을 몰아서 한번에 전송
									transfer_content += "#f#" + sa_file_list[i] + "@" + s_local_address + "%"
											+ new File(pathname + sa_file_list[i]).length() + ":::";
								}
							} // if
						} // for
							// 전송할 내용이 있으면
						if (isExist == true) {
							// 마지막 구분자 :::는 제거
							transfer_content = transfer_content.substring(0, transfer_content.length() - 3);
							// 한번에 몰아서 전송
							out.println(transfer_content);
						}
					} else if (in_msg.startsWith("#c#")) {
						out.println("#c#");
					} else if (in_msg.startsWith("#r#")) {
						// 넘어온 in_msg의 형태가 #r##f#README.html@127.0.0.1:::#f#README.txt@127.0.0.1
						// 앞의 #r# 제거
						String result = in_msg.substring(3);
						// 따라서 구분자 ":::" 로 나누어주자.
//						String[] msg_token_array = result.split(":::");
						// #f#README.html@127.0.0.1:::#f#README.txt@127.0.0.1
						result += ":::";
						String temp;
						int i = 0;

						// split이 안되는 곳도 있기 때문에 while문으로 split 수행
						while (true) {
							int a = result.indexOf(":::");
							// 더 이상 split할 내용이 없으면 while문 break
							if (a == -1)
								break;
							temp = result.substring(0, a); // index 0~a까지
							// 앞에 #f# 제거
							temp = temp.substring(3);
							// 검색된 결과 내용을 listModel에 추가
							receiveListModel.addElement(temp);
							// 첫 번쨰 결과물 선택
							if (i == 0)
								receiveList.setSelectedIndex(0);
							// System.out.println("1 : " + temp);
							result = result.substring(a + 3);
							i++;
						}
					} // if-else
				} else {
					break;
				} // if-else
			} // while
		} catch (Exception e) {

		}
	}// run

	// keyListener 관련 : 검색창에서 enter을 입력했을 경우
	public void keyPressed(KeyEvent e) { // 키를 눌렀을 때
		if (e.getKeyCode() == 10) { // enter
			// TextField에서 enter 키 쳐도 검색
			if (shareFolderSet) {
				search();
			} else {
				Alert("Fale", "공유폴더를 지정해주세요");
			}

		}
	}

	public void keyTyped(KeyEvent e) { // 키 타입
	}

	public void keyReleased(KeyEvent e) { // 키를 뗏을때
	}

	public void mouseClicked(MouseEvent e) { // 클릭했을 때
		// 더블클릭 했을 때 download되게함
		if (e.getClickCount() >= 2) {
			download();
		}
	}

	public void mousePressed(MouseEvent e) { // 눌러졌을 때

	}

	public void mouseReleased(MouseEvent e) { // 눌러진 버튼이 떼어질 때

	}

	public void mouseEntered(MouseEvent e) { // 마우스가 들어왔을 때

	}

	public void mouseExited(MouseEvent e) { // 내려올 때

	}

	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		String selectedDonwList;
		File downListfile;

		if (command.equals("server_ip")) {
			dialog = new JDialog(this, "Server IP 입력", true);
			JLabel l_server_ip = new JLabel("Server IP 를 입력하세요.(예: 203.252.123.1)");
			jtf_server_ip = new JTextField(15);
			// JTextField 에 로컬 IP 라도 출력해주자~
			InetAddress addr = null;
			try {
				addr = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// ue.printStackTrace();
			}
			jtf_server_ip.setText(addr.getHostAddress());
			JButton b_server_ip = new JButton("서버 연결");
			b_server_ip.addActionListener(this);
			b_server_ip.setActionCommand("server_ip_connect_creation");
			JPanel p_server_ip = new JPanel(new BorderLayout());
			p_server_ip.add("North", l_server_ip);
			p_server_ip.add("Center", jtf_server_ip);
			p_server_ip.add("South", b_server_ip);
			dialog.setLocation(300, 100);
			dialog.setSize(380, 110);
			dialog.setContentPane(p_server_ip);
			dialog.show();
		}
		if (command.equals("directory")) {
			shareFolderSet = true;
			directory_choose("Sharing");
		} else if (command.equals("save_directory")) {
			downFolderSet = true;
			directory_choose("Save");
		} else if (command.equals("search")) {
			if (shareFolderSet) {
				search();
			} else {
				Alert("Fail", "공유폴더를 지정해주세요");
			}
		} else if (command.equals("download")) {
			download();
		} else if (command.equals("server_ip_connect_creation")) {
			String temp_host_address = jtf_server_ip.getText();
			// 기존의 connection이 없다면, 연결 가능.
			if (in == null) {
				// 입력값 앞, 뒤 공백 제거
				host_address = temp_host_address.trim();
				int return_result = ConnectCreation();
				if (return_result == 1) {
					dialog.hide();
					Alert("OK", "성공적으로 연결되었습니다.");
					serverState.setText("서버 연결 성공");
				}
			} else {
				dialog.hide();
				Alert("Fail", "이미 연결되어 있어서 Server IP 변경이 불가능합니다.");
			}
		}
		if (command.equals("openFile")) {
			// 파일 열기
			selectedDonwList = (savepathname + downloadList_filename).trim();
			downListfile = new File(selectedDonwList);
			openFile(downListfile);
		} else if (command.equals("openFolder")) {
			// 저장폴더 열기
			openFolder();
		} else if (command.equals("deleteOnList")) {
			// 다운로드 목록에서 제거
			selectedDonwList = (savepathname + downloadList_filename).trim();
			downListfile = new File(selectedDonwList);
			deleteOnDownList(downListfile);
		}
	}

	public void search() {
		// search 메소드
		receiveListModel.removeAllElements();
		// receiveListModel.clear();
		String filename = jtf_search.getText();
		// 클라이언트가 서버에게 검색 요청하는 내용을 보냄
		out.println("#s#" + filename);
	}

	public void Alert(String alert_title, String alert_message) {
		// alert 메소드
		dialog = new JDialog(this, alert_title, true);
		JLabel lll = new JLabel(alert_message);
		lll.setVerticalTextPosition(SwingConstants.CENTER);
		lll.setHorizontalTextPosition(SwingConstants.CENTER);
		JPanel ttt = new JPanel();
		ttt.add(lll);
		dialog.setLocation(180, 80);
		dialog.setSize(320, 100);
		dialog.setContentPane(ttt);
		dialog.show();
	}

	public int download() {
		// download 메소드
		if (receiveListModel.size() < 1) {
			Alert("경고", "검색된 결과가 없어서 다운받을 수 없습니다.");
			return 0;
		}
		downFileName.setVisible(false);
		downBar.setVisible(true);
		System.out.println("downloading : " + download_filename.split("%")[0]);
		String[] data_set = result_divide(download_filename.split("%")[0]);
		// data_set[0] 은 다운받을 파일명
		// data_set[1] 은 다운받을 ip address
		Thread dt = new Thread(new download_thread(data_set[0], data_set[1]));
		dt.start();
		return 1;
	}

	// 파일 열기
	public void openFile(File file) {
		try {
			if (file.exists()) {
				desktop.open(file);
			} else {
				Alert("Fail", "파일이 존재하지 않습니다.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 저장폴더 열기
	public void openFolder() {
		File saveFolder = new File(savepathname);
		try {
			if (saveFolder.exists()) {
				desktop.open(saveFolder);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 다운로드 목록에서 삭제
	public void deleteOnDownList(File file) {
		int index = downList.getSelectedIndex();

		if (file.getName().equals(downloadList_filename)) {
			index--;
			if (index == -1) {
				index++;
			} else {
				downList.setSelectedIndex(index++);
			}
			downListModel.remove(index);
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		try {
			if (e.getValueIsAdjusting() == false) {
				download_filename = receiveList.getSelectedValue().toString();
				downFileName.setText(download_filename.split("@")[0]);
			} // if
		} catch (NullPointerException e1) {

		}
	}

	class downListEvent implements ListSelectionListener, MouseListener {

		public void valueChanged(ListSelectionEvent e) {
			if (downList.getSelectedIndex() >= 0)
				downloadList_filename = downList.getSelectedValue().toString();
		}

		public void mouseClicked(MouseEvent e) {
			// 우클릭
			if (SwingUtilities.isRightMouseButton(e)) {
				pMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public void mousePressed(MouseEvent e) {

		}

		public void mouseReleased(MouseEvent e) {

		}

		public void mouseEntered(MouseEvent e) {

		}

		public void mouseExited(MouseEvent e) {

		}
	}

	class P2p_server extends Thread {
		ServerSocket p2p_server_socket;
		Socket p2p_client;
		PrintWriter requestor;

		public P2p_server(ServerSocket ss) {
			p2p_server_socket = ss;
			start();
		}

		public synchronized void setRequestor(PrintWriter requestor) {
			this.requestor = requestor;
		}

		public void run() {
			while (true) {
				try {
					p2p_client = p2p_server_socket.accept();
					System.out.println("p2p_server run");
				} catch (Exception ex) {

				}

				P2p_connection pc = new P2p_connection(p2p_client, this);
				pc.start();
			}
		}
	}

	class P2p_connection extends Thread {
		Socket socket;
		P2p_server p2p_server;
		BufferedReader in;
		FileInputStream fis;
		File file;
		String file_name;
		BufferedOutputStream out;

		public P2p_connection(Socket s, P2p_server p2p) {
			socket = s;
			p2p_server = p2p;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new BufferedOutputStream(socket.getOutputStream());
			} catch (Exception ex) {

			}
		}

		public void run() {
			String msg = "";
			System.out.println("p2p_connection run");
			try {
				while (true) {
					msg = in.readLine();
					if (msg != null) {
						System.out.println("incoming msg : " + msg);
						if (msg.startsWith("#r#")) {
							file_name = msg.substring(3);
							file = new File(pathname + file_name);
							fis = new FileInputStream(file);
							int c;
							while ((c = fis.read()) != -1) {
								out.write(c);
							}
							out.flush();
							break;
						}
					} else {
						break;
					}
				}
			} catch (Exception ex) {
				System.err.println("in the P2p_connection : " + ex);
			} finally {
				try {
					out.close();
					fis.close();
					in.close();
				} catch (Exception exc) {
					System.err.println("in the P2p_connection finally : " + exc);
				}
			}
		}

	}

	// 다운 받고 있을 때, 다른 일을 할 수 있도록 하기위해 쓰레드 구현
	class download_thread implements Runnable {
		String filename, p2p_address;
		Socket socket;
		BufferedInputStream in;
		PrintWriter out;
		File file;
		FileOutputStream fos;
		double fileSize, size = 0;

		download_thread(String filename, String p2p_address) {
			this.filename = filename;
			this.p2p_address = p2p_address;
		}

		public void run() {
			try {
				String download_filename;
				download_filename = savepathname + filename;
				file = new File(download_filename);
				downListModel.addElement(filename);
				fos = new FileOutputStream(file);
				socket = new Socket(p2p_address, p2p_port_number);
				in = new BufferedInputStream(socket.getInputStream());
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println("#r#" + filename);
				fileSize = Double.valueOf(receiveList.getSelectedValue().toString().split("%")[1]);
				int c;
				downBar.setValue(0);
				while ((c = in.read()) != -1) {
					size += c;
					downBar.setValue((int) ((size * 100 / fileSize) / 128));
					System.out.println((int) ((size * 100 / fileSize) / 128) + "%");
					fos.write(c);
				} // while
				downBar.setValue(100);
				System.out.println("100%");
				Alert("OK", "파일을 성공적으로 다운로드 하였습니다.");
				downBar.setVisible(false);
				downFileName.setVisible(true);
			} catch (Exception e) {
				System.err.println(e);
			} finally {
				try {
					fos.close();
					in.close();
					out.close();
					socket.close();
				} catch (Exception ex) {

				} // try-catch
			} // try-catch-finally
		}
	}

	public static void main(String[] args) {
		P2pClient client = new P2pClient();
		client.run();
	}
}
