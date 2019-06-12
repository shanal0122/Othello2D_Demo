import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.lang.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

public class OthClient extends JFrame implements MouseListener, WindowListener{
	private JButton buttonArray[][];//ボタン用の配列
	private JButton pass, giveUp, judgeWinner ;
	private final int vec[][] = {{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};//ボタン周りの方向ベクトル
	private JLabel textMsg1, textMsg2, textMsg3;
	private int myColor;
	private int myTurn;//1が自分のターン、0が相手のターン
	private boolean canput;//置けるか置けないかを判定するため
	private Container c;
	private ImageIcon blackIcon, whiteIcon, greenIcon;
	private ImageIcon myIcon, yourIcon;
	PrintWriter out;//出力用のライター

	public OthClient(){
		whiteIcon = new ImageIcon("White.jpg");
		blackIcon = new ImageIcon("Black.jpg");
		greenIcon = new ImageIcon("Green.jpg");

		//IPアドレスを入力するダイアログ
		String ipAddress = JOptionPane.showInputDialog(null,"サーバーのIPアドレスを入力してください。localhostならば何も入力しないでください。","IPアドレスの入力",JOptionPane.QUESTION_MESSAGE);
		if(ipAddress.equals("")){
			ipAddress = "localhost";
		}

		//名前の入力ダイアログ
		String myName = JOptionPane.showInputDialog(null,"名前を入力してください","名前の入力",JOptionPane.QUESTION_MESSAGE);
		if(myName.equals("")){
			myName = "No name";
		}
		//ウィンドウを作成する
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//ウィンドウを閉じるときの設定
	  setTitle("Othello   YourName : "+myName);
		setSize(660,810);
		setLocationRelativeTo(null);
		c = getContentPane();//フレームのペインを取得
		c.setLayout(null);//レイアウトマネージャーを無効に
		addWindowListener(this);

		//テキストの生成
		textMsg1 = new JLabel();//手番を表示
		textMsg2 = new JLabel();//コマが置けない場合の警告
		textMsg3 = new JLabel();//試合途中で終局した場合に表示
		c.add(textMsg1);
		c.add(textMsg2);
		c.add(textMsg3);
		textMsg1.setBounds(10,2,700,30);
		textMsg1.setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,25));
		textMsg2.setBounds(300,2,700,30);
		textMsg2.setForeground(Color.RED);
		textMsg2.setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,25));
		textMsg3.setBounds(400,680,700,30);
		textMsg3.setForeground(Color.RED);
		textMsg3.setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,25));
		//ボタンの生成
		setBoard();
		//サーバに接続
		Socket socket = null;
		try{
			socket = new Socket(ipAddress,10000);
		} catch (UnknownHostException e) {
			System.err.println("ホストの IP アドレスが判定できません: " + e);//他PCとつなぐ時用
		} catch (IOException e) {
			 System.err.println("エラーが発生しました: " + e);
		}

		MesgRecvThread mrt = new MesgRecvThread(socket, myName);//受信用のスレッドの作成
		mrt.start();//スレッドを動かす（Runが動く）
	}

	//メッセージ受信のためのスレッド
	public class MesgRecvThread extends Thread{

		Socket socket;
		String myName;

		public MesgRecvThread(Socket s, String n){
			socket = s;
			myName = n;
		}

		//通信状況を監視し，受信データによって動作する
		public void run(){
			try{
				InputStreamReader sisr = new InputStreamReader(socket.getInputStream());
				BufferedReader br = new BufferedReader(sisr);
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println(myName);//接続の最初に名前を送る
				String myNumberStr = br.readLine();
				int myNumberInt = Integer.parseInt(myNumberStr);
				//myColor、myturnの決定（先攻は白）
				if(myNumberInt % 2 != 0){
					myColor = 1;
					myIcon = whiteIcon;
				  yourIcon = blackIcon;
					myTurn = 1;
					textMsg1.setText("Your Turn!");
					}else{
					myColor = 0;
					myIcon = blackIcon;
					yourIcon = whiteIcon;
					myTurn = 0;
					textMsg1.setText("Opponent's Turn.");
				}
				while(true){
					String inputLine = br.readLine();//データを一行分だけ読み込む
					if (inputLine != null) {
						System.out.println(inputLine);//デバック用
						String[] inputTokens = inputLine.split(" ");	//入力データを解析
						String cmd = inputTokens[0];//コマンド（操作を指定）
						//盤上のマスが押された時
						if(cmd.equals("PLACE")){
							String theName = inputTokens[1];//ボタンの名前（０〜６３）
							int thenum = Integer.parseInt(theName);
							int thenumi = thenum / 8;
							int thenumj = thenum % 8;
							int theColor = Integer.parseInt(inputTokens[2]);//送られてきた色
							//アイコンを設置
							int flipTimes;
							if(theColor == myColor){
								buttonArray[thenumi][thenumj].setIcon(myIcon);
								for(int t=0;t<8;t++){
									flipTimes = flipButton(myIcon,t,thenumi,thenumj);
									for(int s=1;s<=flipTimes;s++){
										buttonArray[thenumi+vec[t][0]*s][thenumj+vec[t][1]*s].setIcon(myIcon);
									}
								}
								textMsg1.setText("Opponent's Turn.");
							}
							else{
								buttonArray[thenumi][thenumj].setIcon(yourIcon);
								for(int t=0;t<8;t++){
									flipTimes = flipButton(yourIcon,t,thenumi,thenumj);
									for(int s=1;s<=flipTimes;s++){
										buttonArray[thenumi+vec[t][0]*s][thenumj+vec[t][1]*s].setIcon(yourIcon);
									}
								}
								textMsg1.setText("Your Turn!");
							}
						}
						//Pass.が押された時
						if(cmd.equals("PASS")){
							System.out.println(inputLine);//デバック用
							int theColor = Integer.parseInt(inputTokens[1]);//送られてきた色
							if(theColor == myColor){
								textMsg1.setText("Opponent's Turn.");
								textMsg2.setText("Your turn is passed.");
								tM2Eraser();
							} else {
								textMsg1.setText("Your Turn!");
								textMsg2.setText("Opponent's turn is passed.");
								tM2Eraser();
							}
						}
						//GiveUpが押されたり対戦者がウィンドウを閉じた時
						if(cmd.equals("GIVEUP")){
							int theColor = Integer.parseInt(inputTokens[1]);
							if(theColor == myColor){
								showResalt_Give(myIcon);
								textMsg3.setText("Opponent gave up.");
							} else {
								showResalt_Give(yourIcon);
								textMsg3.setText("You gave up.");
							}
						}
						//どちらも何も置けないという理由でJudge which player win.が押された時
						if(cmd.equals("JUDGE")){
							int count[] = countIcon();
							showResalt_Judge(count);
						}
			  		myTurn = 1 - myTurn;//ターン交代
					}else{
						break;
					}

				}
				socket.close();
			} catch (IOException e){
				System.err.println("エラーが発生しました: " + e);
			}
		}
	}

	public static void main(String[] args){
		OthClient net = new OthClient();
		net.setVisible(true);
	}





	//以下細々としたメソッド
  //盤面の初期設定
	public void setBoard(){
		buttonArray = new JButton[8][8];
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				buttonArray[i][j] = new JButton(greenIcon);
				c.add(buttonArray[i][j]);//ペインに貼り付ける
				buttonArray[i][j].setBounds(10+j*80,30+i*80,80,80);//(x座標，y座標,xの幅,yの幅）
				buttonArray[i][j].addMouseListener(this);
				buttonArray[i][j].setActionCommand(Integer.toString(8*i+j));//ボタンに配列の情報を付加
			}
		}
		buttonArray[3][3].setIcon(blackIcon);
		buttonArray[3][4].setIcon(whiteIcon);
		buttonArray[4][3].setIcon(whiteIcon);
		buttonArray[4][4].setIcon(blackIcon);
		//パスするボタンの生成
		pass = new JButton("PASS");
		c.add(pass);
		pass.setBounds(280,680,100,100);
		pass.setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,30));
		pass.addMouseListener(this);
		//ギブアップするボタンの生成
		giveUp = new JButton(">>Give Up<<");
		c.add(giveUp);
		giveUp.setBounds(30,730,100,50);
		giveUp.setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,12));
		giveUp.setForeground(Color.MAGENTA);
		giveUp.addMouseListener(this);
		//勝敗をジャッジするボタンの生成
		judgeWinner = new JButton("judge which player win.");
		c.add(judgeWinner );
		judgeWinner .setBounds(450,730,200,50);
		judgeWinner .setFont(new Font("HGP創英角ポップ体",Font.BOLD | Font.ITALIC,12));
		judgeWinner .setForeground(Color.GRAY);
		judgeWinner .addMouseListener(this);
	}

	//whichIconをコマを置けるかどうかの判定(theIconは置く位置にもともとあったアイコン)
	public boolean checkButton(Icon whichIcon, Icon theicon, int irow, int jcolumn){
		if(theicon != greenIcon){return false;}
		//周りの8マスを確認
		for(int t=0;t<8;t++){
			if(flipButton(myIcon, t, irow, jcolumn) != 0){return true;}
		}
		return false;
	}

	//各座標におけるvec(各方向)のコマを裏返せる個数を返す(1つも裏返せない場合は0を返す)
	public int flipButton(Icon aicon, int t, int irow, int jcolumn){
		Icon sbjIcon, objIcon;
		if(aicon == myIcon){
			sbjIcon = myIcon;
			objIcon = yourIcon;
		} else {
			sbjIcon = yourIcon;
			objIcon = myIcon;
		}
		int flipNum = 0;
		int i = irow;
		int j = jcolumn;
		loop: for(int s=0;s<7;s++){//裏返る個数は最大6個
			i += vec[t][0];
			j += vec[t][1];
			try{
				if(buttonArray[i][j].getIcon() == objIcon){
					flipNum++;
					continue loop;
				}else if (buttonArray[i][j].getIcon() == sbjIcon) {
					break;
				}else{
					flipNum = 0;
					break;
				}
			} catch(ArrayIndexOutOfBoundsException e) {
				flipNum = 0;
				break;
			}
		}
		return flipNum;
	}

	//盤上の各Iconの数を測定([0]:green, [1]:black, [2]:white)
	public int[] countIcon(){
		int counter[] = {0,0,0};
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
			  if(buttonArray[i][j].getIcon() == greenIcon){
					counter[0]++;
				} else if(buttonArray[i][j].getIcon() == blackIcon){
					counter[1]++;
				} else {
					counter[2]++;
				}
			}
		}
		return counter;
	}

	//judgeWinnerが押された時勝敗を判定する
	public void showResalt_Judge(int count[]){
		//勝った方がほうがtheIcon。引き分けならgreenIcon
		Icon theIcon;
		if(count[1] > count[2]){
			theIcon = blackIcon;
		} else if(count[1] < count[2]){
			theIcon = whiteIcon;
		} else {
			theIcon = greenIcon;
		}
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				int k = 8*i+j;
				if(myIcon == blackIcon){
					if(k<count[1]){
						buttonArray[i][j].setIcon(blackIcon);
					} else if(k>=i && k<count[1]+count[2]){
						buttonArray[i][j].setIcon(whiteIcon);
					} else {
						buttonArray[i][j].setIcon(greenIcon);
					}
				} else {
					if(k<count[2]){
						buttonArray[i][j].setIcon(whiteIcon);
					} else if(k>=i && k<count[1]+count[2]){
						buttonArray[i][j].setIcon(blackIcon);
					} else {
						buttonArray[i][j].setIcon(greenIcon);
					}
				}
				buttonArray[i][j].removeMouseListener(this);
			}
		}
		if(theIcon == myIcon){
			textMsg1.setText("You win!");
			textMsg2.setText("congratulations!!!");
			textMsg3.setText(count[1]+" vs. "+count[2]);
		} else if(theIcon == yourIcon){
			textMsg1.setText("You lose.");
			textMsg2.setBounds(130,2,700,30);
			textMsg2.setText("なんで負けたか明日までに考えといてください");
			textMsg3.setText(count[1]+" vs. "+count[2]);
		} else {
			textMsg1.setText("Draw");
			textMsg2.setText("It was good game.");
			textMsg3.setText("32 vs. 32");
		}
		pass.setEnabled(false);
		pass.removeMouseListener(this);
		giveUp.setEnabled(false);
		giveUp.removeMouseListener(this);
		judgeWinner.setEnabled(false);
		judgeWinner.removeMouseListener(this);
	}

	//giveUpが押されたり接続が切られたりした時に勝敗を表示して終わる。run()も終了する。theIconの人が勝者
	public void showResalt_Give(Icon theIcon){
		for(int i=0;i<8;i++){
			for(int j=0;j<8;j++){
				buttonArray[i][j].setIcon(theIcon);
				buttonArray[i][j].removeMouseListener(this);
			}
		}
		if(theIcon == myIcon){
			textMsg1.setText("You win!");
			textMsg2.setText("congratulations!!!");
		} else {
			textMsg1.setText("You lose.");
			textMsg2.setBounds(130,2,700,30);
			textMsg2.setText("なんで負けたか明日までに考えといてください");
		}
		pass.setEnabled(false);
		pass.removeMouseListener(this);
		giveUp.setEnabled(false);
		giveUp.removeMouseListener(this);
		judgeWinner.setEnabled(false);
		judgeWinner.removeMouseListener(this);
	}

  //textMsg2を１秒後に消去
	public void tM2Eraser(){
		TimerTask task =new TimerTask(){
			public void run(){
				textMsg2.setText(" ");
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 1000);
	}

	//クリック時の動作
	public void mouseClicked(MouseEvent e){
		JButton theButton = (JButton)e.getComponent();//クリックしたオブジェクトを得る
		if(theButton == giveUp){
			int yourColor = 1 - myColor;
			String msg = "GIVEUP"+" "+yourColor;
			out.println(msg);//送信データをバッファに書き出す
			out.flush();//送信データをフラッシュする
		} else if(theButton == judgeWinner){
			int count[] = countIcon();
			//両者ともにコマを置く場所が一つもないかの確認(my,your両方確かめる)
			canput = false;
			for(int i=0;i<8;i++){
				for(int j=0;j<8;j++){
					Icon enyIcon = buttonArray[i][j].getIcon();
					if(checkButton(myIcon,enyIcon,i,j)){
						canput = true;
					}
				}
			}
			for(int i=0;i<8;i++){
				for(int j=0;j<8;j++){
					Icon enyIcon = buttonArray[i][j].getIcon();
					if(checkButton(yourIcon,enyIcon,i,j)){
						canput = true;
					}
				}
			}
			if(count[0] == 0){
				showResalt_Judge(count);
			} else if(canput == false){
				String msg = "JUDGE";
				out.println(msg);//送信データをバッファに書き出す
				out.flush();//送信データをフラッシュする
				textMsg3.setText("No one can put.");
			} else {
				textMsg2.setText("This game is not over yet.");
				tM2Eraser();
			}
		} else {
			if(myTurn == 1){
				if(theButton == pass){
					//おける場所が一つもないかどうかの確認(canput == falseならおける場所がないのでパス可)
					canput = false;
					for(int i=0;i<8;i++){
						for(int j=0;j<8;j++){
							Icon enyIcon = buttonArray[i][j].getIcon();
							if(checkButton(myIcon,enyIcon,i,j)){
								canput = true;
							}
						}
					}
					if(canput == false){
						String msg = "PASS"+" "+myColor;//送信情報を作成する
						//サーバに情報を送る
						out.println(msg);//送信データをバッファに書き出す
						out.flush();//送信データをフラッシュする
					} else {
						textMsg2.setText("You can put it somewhere.");
						tM2Eraser();
					}
				} else {//GreenかBlackかWhiteを押した場合
					Icon theIcon = theButton.getIcon();//アイコンのイメージ
					String theArrayIndex = theButton.getActionCommand();//配列の番号
					int theValue = Integer.parseInt(theArrayIndex);
					int theValuei = theValue / 8;
					int theValuej = theValue % 8;
					if(checkButton(myIcon, theIcon, theValuei, theValuej)){
						String msg = "PLACE"+" "+theArrayIndex+" "+myColor;//送信情報を作成する
						//サーバに情報を送る
						out.println(msg);//送信データをバッファに書き出す
						out.flush();//送信データをフラッシュする
						//repaint();//再描画
					}else{
						textMsg2.setText("You can't put it there.");
						tM2Eraser();
						//repaint();//再描画
					}
				}
			} else {
				textMsg2.setText("It's opponent turn.");
				tM2Eraser();
			}
		}
	}

	public void mouseEntered(MouseEvent e){
	}

	public void mouseExited(MouseEvent e){
	}

	public void mousePressed(MouseEvent e){
	}

	public void mouseReleased(MouseEvent e){
	}

	public void windowOpened(WindowEvent e){
	}

  public void windowClosing(WindowEvent e){
		int yourColor = 1 - myColor;
		String msg = "GIVEUP"+" "+yourColor;
		out.println(msg);//送信データをバッファに書き出す
		out.flush();//送信データをフラッシュする
		System.out.println(msg);
	}

  public void windowClosed(WindowEvent e){
	}

  public void windowIconified(WindowEvent e){
	}

  public void windowDeiconified(WindowEvent e){
	}

  public void windowActivated(WindowEvent e){
	}

  public void windowDeactivated(WindowEvent e) {
  }
}
