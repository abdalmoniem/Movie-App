package org.hifnawy.base.providers.media.response.models;

import java.util.ArrayList;

import org.hifnawy.base.providers.media.MediaProvider;
import org.hifnawy.base.providers.media.models.Media;
import org.hifnawy.base.providers.subs.SubsProvider;

public abstract class DetailsResponse<T extends ResponseItem> {

    public DetailsResponse() {
    }

    public abstract ArrayList<Media> formatDetailForPopcorn(T responseItem, MediaProvider mediaProvider, SubsProvider subsProvider);
}