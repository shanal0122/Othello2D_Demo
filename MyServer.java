import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;

//スレッド部（各クライアントに応じて）
class ClientProcThread extends Thread {
	private int number;//自分の番号
	private Socket incoming;
	private InputStreamReader myIsr;
	private BufferedReader myIn;
	private PrintWriter myOut;
	private String myName;//接続者の名前

	public ClientProcThread(int n, Socket i, InputStreamReader isr, BufferedReader in, PrintWriter out) {
		number = n;
		incoming = i;
		myIsr = isr;
		myIn = in;
		myOut = out;
	}

	public void run() {
		try {
			myOut.println(number);//初回だけ呼ばれる

			myName = myIn.readLine();//初めて接続したときの一行目は名前

			while (true) {//無限ループで，ソケットへの入力を監視する
				String str = myIn.readLine();
				//System.out.println("Received from client No."+number+"("+myName+"), Messages: "+str);//デバック用
				if (str != null) {//このソケット（バッファ）に入力があるかをチェック
					MyServer.SendAll(str, myName);//サーバに来たメッセージは接続しているクライアント全員に配る
				}else{
					break;
				}
			}
		} catch (Exception e) {
			//ここにプログラムが到達するときは，接続が切れたとき
			System.out.println("Disconnect from client No."+number+"("+myName+")");//デバック用
			MyServer.SetFlag(number, false);//接続が切れたのでフラグを下げる
		}
	}
}

class MyServer{

	private static int maxConnection=100;//最大接続数
	private static Socket[] incoming;//受付用のソケット
	private static boolean[] flag;//接続中かどうかのフラグ
	private static InputStreamReader[] isr;//入力ストリーム用の配列
	private static BufferedReader[] in;//バッファリングをによりテキスト読み込み用の配列
	private static PrintWriter[] out;//出力ストリーム用の配列
	private static ClientProcThread[] myClientProcThread;//スレッド用の配列
	private static int member;//接続しているメンバーの数

	//全員にメッセージを送る
	public static void SendAll(String str, String myName){
		//送られた来たメッセージを接続している全員に配る
		for(int i=1;i<=member;i++){
			if(flag[i] == true){
				out[i].println(str);
				out[i].flush();//バッファをはき出す＝＞バッファにある全てのデータをすぐに送信する
				System.out.println("Send messages to client No."+i);//デバック用
			}
		}
	}

	//フラグの設定を行う
	public static void SetFlag(int n, boolean value){
		flag[n] = value;
	}

	//mainプログラム
	public static void main(String[] args) {
		//必要な配列を確保する
		incoming = new Socket[maxConnection];
		flag = new boolean[maxConnection];
		isr = new InputStreamReader[maxConnection];
		in = new BufferedReader[maxConnection];
		out = new PrintWriter[maxConnection];
		myClientProcThread = new ClientProcThread[maxConnection];

		int n = 1;
		member = 0;//誰も接続していないのでメンバー数は０

		try {
			System.out.println("The server has launched!");
			ServerSocket server = new ServerSocket(10000);//10000番ポートを利用する
			while (true) {
				incoming[n] = server.accept();
				flag[n] = true;
				System.out.println("Accept client No." + n);
				//必要な入出力ストリームを作成する
				isr[n] = new InputStreamReader(incoming[n].getInputStream());
				in[n] = new BufferedReader(isr[n]);
				out[n] = new PrintWriter(incoming[n].getOutputStream(), true);

				myClientProcThread[n] = new ClientProcThread(n, incoming[n], isr[n], in[n], out[n]);//必要なパラメータを渡しスレッドを作成
				myClientProcThread[n] .start();//スレッドを開始する
				member = n;//メンバーの数を更新する
				n++;
			}
		} catch (Exception e) {
			System.err.println("ソケット作成時にエラーが発生しました: " + e);
		}
	}
}
