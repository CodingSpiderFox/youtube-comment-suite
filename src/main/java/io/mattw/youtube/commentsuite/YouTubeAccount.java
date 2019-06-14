package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import io.mattw.youtube.commentsuite.db.CommentDatabase;
import io.mattw.youtube.commentsuite.db.YouTubeChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;

/**
 * Combination of YouTubeChannel and OAuth2Tokens as sign-in.
 * Data stored in Config 'commentsuite.json'
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeAccount implements Serializable {

    private transient Logger logger = LogManager.getLogger(this);

    private String username, channelId, thumbUrl;
    private OAuth2Tokens tokens;

    public YouTubeAccount() {
        // default constructor
    }

    /**
     * Only used with "YouTube Account Sign-in," otherwise initialized by Gson & Config.
     */
    public YouTubeAccount(OAuth2Tokens tokens) {
        this.tokens = tokens;

        updateData();
    }

    /**
     * Using the OAuth2Tokens passed in, query the YouTube API to get the "mine"
     * channel for those tokens.
     *
     * Pushes the channel to the database.
     */
    void updateData() {
        CommentDatabase database = FXMLSuite.getDatabase();
        YouTube youtube = FXMLSuite.getYouTube();

        logger.debug("Getting account data for [accessToken={}]", this.tokens.getAccessToken().substring(0,10)+"...");

        try {
            ChannelListResponse cl = youtube.channels().list("snippet")
                    .setOauthToken(getTokens().getAccessToken())
                    .setMine(true)
                    .execute();

            Channel cli = cl.getItems().get(0);

            this.channelId = cli.getId();

            if(cli.getSnippet() != null) {
                this.username = cli.getSnippet().getTitle();
                this.thumbUrl = cli.getSnippet().getThumbnails().getMedium().getUrl();

                try {
                    YouTubeChannel channel = new YouTubeChannel(cli);
                    database.insertChannels(Collections.singletonList(channel));
                    database.commit();
                } catch (SQLException e) {
                    logger.error("Unable to insert account channel into database.", e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to query for account channel info.", e);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String toString() {
        return username;
    }

    public OAuth2Tokens getTokens() {
        return tokens;
    }

    public void setTokens(OAuth2Tokens tokens) {
        this.tokens = tokens;
    }

    public boolean equals(Object o) {
        return o instanceof YouTubeAccount && ((YouTubeAccount) o).getChannelId() != null && ((YouTubeAccount) o).getChannelId().equals(channelId);
    }
}
