package org.hifnawy.base.providers.media.response.models;

import java.util.ArrayList;
import java.util.List;

import org.hifnawy.base.providers.media.MediaProvider;
import org.hifnawy.base.providers.media.models.Media;
import org.hifnawy.base.providers.subs.SubsProvider;

public abstract class Response<T extends ResponseItem> {

    protected List<T> responseItems;

    public Response(List<T> responseItems) {
        this.responseItems = responseItems;
    }

    public abstract ArrayList<Media> formatListForPopcorn(ArrayList<Media> existingList, MediaProvider mediaProvider, SubsProvider subsProvider);

    //public abstract ArrayList<Media> formatDetailForPopcorn();
}