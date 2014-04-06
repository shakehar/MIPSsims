public class Pipeline {

	private int time = 0;
	private int postMemQueue = -1;
	private int postAluQueue = -1;
	private int preMemQueue = -1;
	private int[] preAluQueue = { -1, -1 };
	private int preAluSize = 0;
	private int preAluQueueHead = 0;
	private int preAluQueueTail = 1;
	private int[] preIssueQueue = { -1, -1, -1, -1 };
	private int preIssueQueueHead = 0;
	private int preIssueTail = 2;
	private int preIssueSize = 0;

	public void MoveItems() {
		PostMemMove();
		PostALUMove();
		PreMemMove();
		PreAluMove();
		PreIssueMove();
		PreIssueInsert();

	}

	private void PreIssueInsert() {
		// TODO Auto-generated method stub

	}

	private void PreIssueMove() {
		// TODO Auto-generated method stub

	}

	private void PreAluMove() {
		// TODO Auto-generated method stub

	}

	private void PreMemMove() {
		if (preMemQueue > 0) {
			if (postMemQueue == -1) {
				postMemQueue = preMemQueue;
				preMemQueue = -1;
			}
		}

	}

	private void PostALUMove() {
		if (postAluQueue > 0) {
			WriteBack(postAluQueue);
			postAluQueue = -1;
		}

	}

	private void PostMemMove() {
		if (postMemQueue > 0) {
			WriteBack(postMemQueue);
			postMemQueue = -1;
		}

	}

	private void WriteBack(int instruction) {
		// TODO Auto-generated method stub

	}

}
