package org.hifnawy.base.providers.media.response;

import java.util.ArrayList;
import java.util.List;

import org.hifnawy.base.providers.media.MediaProvider;
import org.hifnawy.base.providers.media.models.Media;
import org.hifnawy.base.providers.media.response.models.Response;
import org.hifnawy.base.providers.media.response.models.shows.Show;
import org.hifnawy.base.providers.subs.SubsProvider;

public class TVResponse extends Response<Show> {

    public TVResponse(List<Show> responseItems) {
        super(responseItems);
    }

    public ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList, MediaProvider mediaProvider, SubsProvider subsProvider) {
        for (Show item : responseItems) {
            org.hifnawy.base.providers.media.models.Show show = new org.hifnawy.base.providers.media.models.Show();

            show.title = item.getTitle();
            show.videoId = item.getImdbId();
            show.seasons = item.getNumSeasons();
            show.tvdbId = item.getTvdbId();
            show.year = item.getYear();
            if (item.getImages().getPoster() != null && !item.getImages().getPoster().contains("images/posterholder.png")) {
                show.image = item.getImages().getPoster();
                show.fullImage = item.getImages().getPoster().replace("w500", "w1280");
            }
            if (item.getImages().getFanart() != null && item.getImages().getFanart().contains("images/posterholder.png")) {
                show.headerImage = item.getImages().getFanart().replace("w500", "original");
            }
            existingList.add(show);
        }
        return existingList;
    }
}