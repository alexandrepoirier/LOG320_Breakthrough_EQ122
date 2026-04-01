import java.io.*;
import java.net.*;


class Client {
	private static String host = "localhost";
	private static int port = 8888;
	private static Socket MyClient;
	private static BufferedInputStream input;
	private static BufferedOutputStream output;
	private static BufferedReader console;

	private static Board board;
	private static CPUPlayer TheDominator;
	private static int turnCount = 0;

	public static final boolean DEBUG_MODE = true;
	public static PlayMode PLAY_MODE = PlayMode.CPU;

	public static void main(String[] args) {
		Initiliaze(args);

        try {
			MyClient = new Socket(host, port);
			input    = new BufferedInputStream(MyClient.getInputStream());
			output   = new BufferedOutputStream(MyClient.getOutputStream());
			console = new BufferedReader(new InputStreamReader(System.in));
			char cmd;

			while(GameState.getState() != GameState.State.TERMINATED){
				cmd = (char)input.read();

				if (DEBUG_MODE) {
					System.out.println("Command received : " + cmd);
				}

				switch (cmd){
					case '0':
						GameState.setState(GameState.State.TERMINATED);
						break;
					case '1':
						BeginGameAsRed();
						break;
					case '2':
						BeginGameAsBlack();
						break;
					case '3':
						if (ValidateOpponentMove())
							Play();
						break;
					case '4':
						PlayAgain();
						break;
					case '5':
						GameOver();
						break;
					case '\uFFFF':
						GameState.setState(GameState.State.TERMINATED);
				}
			}
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
		}finally{
			System.exit(0);
		}
    }

	private static void Initiliaze(String[] args) {
		// Parse args for command line arguments
		for (String arg : args) {
			if (arg.charAt(0) == '-'){
				String[] argument = arg.substring(1).toLowerCase().split(":");

				switch (argument[0]) {
					case "mode":
						if (argument.length > 1 && argument[1].equals("cpu")){
							PLAY_MODE = PlayMode.CPU;
							if (DEBUG_MODE) System.out.println("Play Mode: CPU");
						}else if (argument.length > 1 && argument[1].equals("manual")){
							PLAY_MODE = PlayMode.MANUAL;
							if (DEBUG_MODE) System.out.println("Play Mode: MANUAL");
						}else{
							System.err.println("'mode' flag was not set : no mode was specified");
						}
						break;
				}
			}
		}
	}

	// Implémentation des commandes

	public static void BeginGameAsRed() throws IOException {
		byte[] buffer = new byte[1024];
		input.read(buffer,0, input.available());
		board = new Board(8, buffer);
		TheDominator = new CPUPlayer(Mark.R);
		System.out.println("Nouvelle partie! Vous jouez rouge.");
		Play();
	}

	public static void BeginGameAsBlack() throws IOException{
		byte[] buffer = new byte[1024];
		input.read(buffer,0, input.available());
		board = new Board(8, buffer);
		TheDominator = new CPUPlayer(Mark.B);
		System.out.println("Nouvelle partie! Vous jouez noir, attendez le coup des rouges...");
	}

	public static void Play() throws IOException{
		GameState.setState(GameState.State.PLAYING);

		turnCount++;
		switch (PLAY_MODE){
			case CPU:
				PlayCPU();
				break;
			case MANUAL:
				PlayManual();
				break;
		}

		GameState.setState(GameState.State.WAITING);
	}

	private static void PlayCPU() throws IOException{
		System.out.println("The computer is computing...");
		Move newMove = TheDominator.getBestMove(board);
        board.play(newMove);
		String  newMoveStr = newMove.toString();
		System.out.println("The computer plays : " + newMove);
		output.write(newMoveStr.getBytes(),0,newMoveStr.length());
		output.flush();
	}

	private static void PlayManual() throws IOException {
		Move newMove = null;

		while (newMove == null) {
			try{
				System.out.print("Entrez votre coup : ");
				newMove = new Move(console.readLine(), TheDominator.getCpuMark());
			}catch (InvalidMoveException e){
				if (DEBUG_MODE) System.err.println(e.getMessage());
				System.out.println("Invalid move, try again. ");
			}
		}

		String newMoveStr = newMove.toString();
		System.out.println("Playing : " + newMoveStr);
		output.write(newMoveStr.getBytes(),0,newMoveStr.length());
		output.flush();
	}

	private static void PlayAgain() throws IOException{
		System.out.println("Votre coup précédent était invalide.");
		Play();
	}

	private static boolean ValidateOpponentMove() throws IOException {
		byte[] buffer = new byte[16];
		input.read(buffer,0, input.available());
		String s = new String(buffer);
		String[] parts = s.split("-");
		Move opponentMove;

		try{
			opponentMove = new Move(parts[0].trim() + parts[1].trim(), TheDominator.getOpponentMark());
            board.play(opponentMove);
		}catch (InvalidMoveException e){
			if(DEBUG_MODE) System.err.println("Invalid opponent move : " + e.getMessage());
			output.write("4".getBytes(),0,1);
			output.flush();
			return false;
		}

		turnCount++;

		return true;
	}

	private static void GameOver() {
		GameState.setState(GameState.State.OVER);

		if (DEBUG_MODE){
			System.out.println("Partie terminée.");
		}
	}

	public static int getTurnCount() {
		return turnCount;
	}
}
