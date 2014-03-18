package torrenthandlers;

import java.util.ArrayList;

// File sizes and speed are in BYTES
// Percent and ratio are floats
// There is more information depending on type (ruTorrent)
// but we don't need it for the app.

public class BaseTorrentHandler {
	public class TorrentFile {
		public String hash;
		public String name;
		public String status;
		public String savepath;
		public String label;
		public long size;
		public float percent;
		public long downloaded;
		public long uploaded;
		public float ratio;
		public long dl;
		public long ul;
		public long eta;
		public long peers_connected;
		public long peers_total;
		public long seeds_connected;
		public long seeds_total;
	};
	
	public ArrayList<TorrentFile> files;
	
	public boolean login(String username, String password, String server, String port) {
		return true;
	}
	
	public boolean updateFileList() {
		if(files != null) {
			files.clear();
		} else {
			files = new ArrayList<TorrentFile>();
		}
		
		return true;
	}
	
	public boolean addURL(String url) {
		return true;
	}
	
	public void start(TorrentFile which) {}
	public void pause(TorrentFile which) {}
	public void resume(TorrentFile which) {}
	public void stop(TorrentFile which) {}
	public void removeAndDelete(TorrentFile which) {}
	public void remove(TorrentFile which) {}
}
