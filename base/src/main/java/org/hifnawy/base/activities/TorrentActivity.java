package org.hifnawy.base.activities;

import org.hifnawy.base.torrent.TorrentService;

public interface TorrentActivity {

	TorrentService getTorrentService();

	void onTorrentServiceConnected();

	void onTorrentServiceDisconnected();
}