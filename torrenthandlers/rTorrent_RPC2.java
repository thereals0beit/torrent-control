package torrenthandlers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;

import com.squareup.okhttp.internal.Base64;

import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;

public class rTorrent_RPC2 extends BaseTorrentHandler {
	private XmlRpcClient client;
	private final static String TAG_FIELD = "custom2";
	private final static String COMMENT_FIELD = "custom1";
	
	private XmlRpcClient createRPCClient(String host, String port, String username, String password) throws MalformedURLException {
        XmlRpcClient rpcClient = new XmlRpcClient("http://" + host + ":" + port + "/RPC2", false);
        rpcClient.setRequestProperty("Authorization", "Basic " + new String(Base64.encode((username + ":" + password).getBytes())));
        return rpcClient;
    }
	
	private Object call(String method) throws XmlRpcFault {
		return client.invoke(method, (Object[]) null);
	}
	
	private Object call(String method, Object a1) throws XmlRpcFault {
		Object[] params = { a1 };
		return client.invoke(method, params);
	}
	
	private Object call(String method, Object a1, Object a2) throws XmlRpcFault {
		Object[] params = { a1, a2 };
		return client.invoke(method, params);
	}
	
	public void call_hash(String method, String hash) {
		try {
			this.call(method, hash);
		} catch (XmlRpcFault e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<HashMap<String, Object>> multicall(String channel, ArrayList<String> params) throws XmlRpcException, XmlRpcFault {
		ArrayList<HashMap<String, Object>> r = new ArrayList<HashMap<String, Object>>();
		
		params.add(0, channel);
		
		XmlRpcArray data = (XmlRpcArray) client.invoke("d.multicall", params);
		
		params.remove(0);
		
		for(int i = 0; i < data.size(); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			
			Object[] info = (Object[]) ((XmlRpcArray) data.toArray()[i]).toArray();
			
			for(int s = 0; s < info.length; s++) {
				map.put((String) params.get(s), info[s]);
			}
			
			r.add(map);
		}
		
		return r;
	}
		
	@Override
	public boolean login(String username, String password, String server, String port) {
		try {
			client = createRPCClient(server, port, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (client != null);
	}
	
	@Override
	public boolean updateFileList() {
		super.updateFileList();
		
		try {
			ArrayList<String> params = new ArrayList<String>();
			
			params.add("d.get_name=");
			params.add("d.get_hash=");
			params.add("d.get_hashing=");
			params.add("d.is_active=");
			params.add("d.is_hash_checking=");
			params.add("d.get_size_chunks=");
			params.add("d.get_completed_chunks=");
			params.add("d.get_" + COMMENT_FIELD + "=");
			params.add("d.get_" + TAG_FIELD + "=");
			params.add("d.get_chunk_size=");
			params.add("d.get_message=");
			params.add("d.get_peers_connected=");
			params.add("d.get_peers_not_connected=");
			params.add("d.get_peers_complete=");
			params.add("d.get_state=");
			params.add("d.get_down_rate=");
			params.add("d.get_up_rate=");
			params.add("d.get_ratio=");
			params.add("d.get_directory=");
			params.add("d.get_complete=");
			params.add("d.is_open=");
			
			ArrayList<HashMap<String, Object>> torrents = multicall("main", params);
			
			for(int i = 0; i < torrents.size(); i++) {
				TorrentFile tf = new TorrentFile();
				
				long is_open = Long.parseLong(torrents.get(i).get("d.is_open=").toString());
				long is_active = Long.parseLong(torrents.get(i).get("d.is_active=").toString());
				long is_complete = Long.parseLong(torrents.get(i).get("d.get_complete=").toString());
				long get_state = Long.parseLong(torrents.get(i).get("d.get_state=").toString());
				long get_hashing = Long.parseLong(torrents.get(i).get("d.get_hashing=").toString());
				long is_hash_checking = Long.parseLong(torrents.get(i).get("d.is_hash_checking=").toString());
				long chunk_size = Long.parseLong(torrents.get(i).get("d.get_chunk_size=").toString());
				long completed_chunks = Long.parseLong(torrents.get(i).get("d.get_completed_chunks=").toString());
				long size_chunks = Long.parseLong(torrents.get(i).get("d.get_size_chunks=").toString());
				long peers_connected = Long.parseLong(torrents.get(i).get("d.get_peers_connected=").toString());
				long peers_not_connected = Long.parseLong(torrents.get(i).get("d.get_peers_not_connected=").toString());
				long peers_complete = Long.parseLong(torrents.get(i).get("d.get_peers_complete=").toString());
				
				tf.name = torrents.get(i).get("d.get_name=").toString();
				tf.hash = torrents.get(i).get("d.get_hash=").toString();
				tf.size = size_chunks * chunk_size;
				tf.percent = (Float.intBitsToFloat((int) completed_chunks * (int) chunk_size) / Float.intBitsToFloat((int) tf.size));
				tf.peers_connected = peers_connected;
				tf.peers_total = peers_connected + peers_not_connected;
				tf.seeds_connected = peers_complete; //???
				tf.seeds_total = peers_complete;
				tf.uploaded = Long.parseLong(torrents.get(i).get("d.get_up_rate=").toString());
				tf.downloaded = Long.parseLong(torrents.get(i).get("d.get_down_rate=").toString());
				tf.eta = (long) ((tf.downloaded > 0) ? Math.floor((size_chunks - completed_chunks) * chunk_size / tf.downloaded) : 0);
				tf.ratio = (float) tf.uploaded / (float) tf.downloaded;
				tf.dl = Long.parseLong(torrents.get(i).get("d.get_down_rate=").toString());
				tf.ul = Long.parseLong(torrents.get(i).get("d.get_up_rate=").toString());
				tf.savepath = torrents.get(i).get("d.get_directory=").toString();
				tf.label = torrents.get(i).get("d.get_" + COMMENT_FIELD + "=").toString(); // Why is comment field the label...?
				tf.peers_connected = Long.parseLong(torrents.get(i).get("d.get_peers_connected=").toString());
				
				if(tf.ratio < 0.0f) { tf.ratio = 0.0f; }
				
				if(get_state == 0) {
					tf.status = "Queued";
				} else if(is_active == 1) {
					if(is_complete == 1) {
						tf.status = "Seeding";
					} else {
						tf.status = "Downloading";
					}
				} else if(is_hash_checking == 1) {
					tf.status = "Checking";
				} else {
					tf.status = "Paused";
				}
				
				files.add(tf);
			}
			
			return true;
		} catch (XmlRpcFault e){
			e.printStackTrace();
		}

		return false;
	}
	
	@Override
	public boolean addURL(String url) {
		return true;
	}
	
	@Override
	public void start(TorrentFile which) {
		call_hash("d.start", which.hash);
	}
	
	@Override
	public void pause(TorrentFile which) {
		call_hash("d.pause", which.hash);
	}
	
	@Override
	public void resume(TorrentFile which) {
		call_hash("d.resume", which.hash);
	}
	
	@Override
	public void stop(TorrentFile which) {
		call_hash("d.stop", which.hash);
	}
	
	@Override
	public void removeAndDelete(TorrentFile which) {
		call_hash("d.delete_tied", which.hash); // May be broken, have not tested yet...
	}
	
	@Override
	public void remove(TorrentFile which) {
		call_hash("d.erase", which.hash);
	}
}
