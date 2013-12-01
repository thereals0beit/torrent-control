package torrenthandlers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.squareup.okhttp.internal.Base64;

public class uTorrent extends BaseTorrentHandler {
	private String savedServer;
	private String savedPort;
	private String loginB64;
	private String GUID;
	private String token;
	private String listCacheId = "0";
	private JSONObject constants;
	
	private String doUrl(String bit) {
		return "http://" + savedServer + ":" + savedPort + "/gui/" + bit;
	}
	
	private String defaultGetRequest(String bit) throws Exception {
		HttpClient h = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(doUrl(bit));
		
		httpPost.addHeader("Authorization", "Basic " + loginB64);
		
		if(GUID != null && GUID.length() != 0) {
			httpPost.addHeader("Cookie", "GUID=" + GUID);
		}
		
		HttpResponse response = h.execute(httpPost);
		HttpEntity entity = response.getEntity();
		InputStream is = entity.getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
		StringBuilder sb = new StringBuilder();
		String line = null;
		while((line = reader.readLine()) != null) {
			sb.append(line);
		}
		is.close();
		return sb.toString();
	}
	
	private Document soupRequest(String bit) throws Exception {
		Connection con = Jsoup.connect(doUrl(bit)).header("Authorization", "Basic " + loginB64);
		
		if(GUID != null && GUID.length() != 0) {
			con.cookie("GUID", GUID);
		}
		
		return con.get();
	}
	
	private boolean parseConstants() throws Exception {
		String constantsString = defaultGetRequest("constants.js");
		
		if(constantsString == null || constantsString.length() == 0)
			return false;
		
		String[] spl = constantsString.split("var CONST=\\{");
		String in = spl[1].substring(0, spl[1].length() - 2);
		this.constants = new JSONObject("{" + in + "}");
		return (this.constants.length() != 0);
	}
	
	private String constantTag(String value) {
		return value.toUpperCase(Locale.ENGLISH);
	}
	
	private String constantTag(String section, String value) {
		return this.constantTag(section + "_" + value);
	}
	
	private void standardAction(TorrentFile which, String action) {
		try {
			String res = defaultGetRequest(
					"?token" + this.token + 
					"&action=" + action + 
					"&hash=" + which.hash + 
					"&list=1" + 
					"&cid=" + listCacheId +
					"&getmsg=1&t=" + System.currentTimeMillis());
			
			updateFileListWithData(res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean updateFileListWithData(String data) throws Exception {
		JSONObject torrentObject = new JSONObject(data);
		listCacheId = torrentObject.getString("torrentc");
		JSONArray torrents = torrentObject.getJSONArray("torrents");

		if(torrents.length() > 0) {
			for(int i = 0; i < torrents.length(); i++) {
				JSONArray torrent = torrents.getJSONArray(i);
				
				TorrentFile f = new TorrentFile();

				String tmpname = torrent.getString(constants.getInt(constantTag("TORRENT", "NAME")));
				String tmppath = torrent.getString(constants.getInt(constantTag("TORRENT", "SAVE_PATH")));
				
				byte[] tmpnameb = tmpname.getBytes("ISO-8859-1");
				byte[] tmppathb = tmppath.getBytes("ISO-8859-1");
				
				f.hash = torrent.getString(constants.getInt(constantTag("TORRENT", "HASH")));
				f.name = new String(tmpnameb, "UTF-8");
				f.status = torrent.getString(constants.getInt(constantTag("TORRENT", "STATUS_MESSAGE")));
				f.savepath = new String(tmppathb, "UTF-8");
				f.size = torrent.getLong(constants.getInt(constantTag("TORRENT", "SIZE")));
				f.percent = (float)torrent.getInt(constants.getInt(constantTag("TORRENT", "PROGRESS")));
				f.downloaded = torrent.getLong(constants.getInt(constantTag("TORRENT", "DOWNLOADED")));
				f.uploaded = torrent.getLong(constants.getInt(constantTag("TORRENT", "UPLOADED")));
				f.ratio = (float)torrent.getInt(constants.getInt(constantTag("TORRENT", "RATIO")));
				f.dl = torrent.getLong(constants.getInt(constantTag("TORRENT", "DOWNSPEED")));
				f.ul = torrent.getLong(constants.getInt(constantTag("TORRENT", "UPSPEED")));
				f.eta = torrent.getLong(constants.getInt(constantTag("TORRENT", "ETA")));
				f.peers_connected = torrent.getLong(constants.getInt(constantTag("TORRENT", "PEERS_CONNECTED")));
				f.peers_total = torrent.getLong(constants.getInt(constantTag("TORRENT", "PEERS_SWARM")));
				f.seeds_connected = torrent.getLong(constants.getInt(constantTag("TORRENT", "SEEDS_CONNECTED")));
				f.seeds_total = torrent.getLong(constants.getInt(constantTag("TORRENT", "SEEDS_SWARM")));
				
				if(f.percent > 0.0f) {
					f.percent = f.percent / 10.0f;
				}
				
				if(f.ratio > 0.0f) {
					f.ratio = f.ratio / 10.0f;
				}
				
				files.add(f);
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean login(String username, String password, String server, String port) {
		loginB64 = new String(Base64.encode((username + ":" + password).getBytes()));
		savedServer = server;
		savedPort = port;
		
		try {
			Document loginResult = soupRequest("index.html");
			
			if(loginResult.getElementById("modalbg") != null) {
				Connection con = Jsoup
						.connect(doUrl("token.html?t=" + String.valueOf(System.currentTimeMillis())))
						.header("Authorization", "Basic " + loginB64);
				
			    Document tokenDocument = con.get();
			    
			    Response response = con.response();
				
			    this.GUID = response.cookies().get("GUID");
			    this.token = tokenDocument.getElementById("token").html();

				if(parseConstants()) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return false;
	}
	
	@Override
	public boolean updateFileList() {
		super.updateFileList();
		
		try {
			String res = defaultGetRequest("?token=" + this.token + "&list=1&cid=" + listCacheId + "&getmsg=1&t=" + System.currentTimeMillis());

			return updateFileListWithData(res);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public boolean addURL(String url) {
		try {
			String res = defaultGetRequest(
					"?token=" + this.token + 
					"&action=add-url&s=" + URLEncoder.encode(url, "UTF-8") + 
					"&t=" + System.currentTimeMillis());
			
			if(res.substring(0, 4).equals("{\"bu")) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public void start(TorrentFile which) {
		standardAction(which, "start");
	}
	
	@Override
	public void pause(TorrentFile which) {
		standardAction(which, "pause");
	}
	
	@Override
	public void stop(TorrentFile which) {
		standardAction(which, "stop");
	}
	
	@Override
	public void removeAndDelete(TorrentFile which) {
		standardAction(which, "removedatatorrent");
	}
	
	@Override
	public void remove(TorrentFile which) {
		standardAction(which, "remove");
	}
}
