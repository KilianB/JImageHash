package com.github.kilianB.examples.nineGagDuplicateDetectionAndMemeCategorizer.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class describing the base instance of a post found on 9 gag.
 * 
 * @author Kilian
 *
 */
public abstract class PostItem {

	private static final Logger LOGGER = Logger.getLogger(PostItem.class.getName());

	// A unique id to differentiate the posts. We want to know how far apart the
	// individual items are.
	private static AtomicInteger globalInt = new AtomicInteger(0);

	/** Title of the post */
	protected String title;

	/** Photo, Gif, Video. */
	protected Type type;

	protected String id; // Id of the post TODO what is ID? not universal accros posts?

	/** Url of the 9 gag page */
	protected String url;

	/** Not safe for work */
	protected boolean nsfw;

	/** Promoted post */
	protected boolean promoted;

	// Extended post foldable? //TODO just for articles?
	protected boolean hasLongPostCover;

	/**
	 * User defined tags
	 */
	protected List<Tag> tags = new ArrayList<>();

	/**
	 * The section this post is associated with (funny, animal ...)
	 */
	protected Section section;

	/** Creation timestamp of the post */
	protected Instant creationTime;

	/** Timestamp of the query */
	protected ZonedDateTime queryTime;
	// Unusued metatags

	@Deprecated
	/** Present but always empty. Unused? */
	protected String sourceUrl;

	protected String descriptionHtml; // empty unused?

	// End metainformation

	// Mutable fields
	// We use arbitrary sections we are not aware of during compilation. Therefore,
	// we can't use enum values
	protected int upvoteCount; // How many upvotes does a post have
	protected int commentCount; // How often did people comment under the picture

	// @since 13.1.2019 got added recently
	protected int downvoteCount;

	/**
	 * New posts vote count is masked. to not influence users
	 */
	protected boolean isVoteMasked;

	public static void setGlobalInt(int newInt) {
		globalInt.set(newInt);
	}
	
	/**
	 * @return the downvoteCount
	 */
	public int getDownvoteCount() {
		return downvoteCount;
	}

	/**
	 * @param downvoteCount the downvoteCount to set
	 */
	public void setDownvoteCount(int downvoteCount) {
		this.downvoteCount = downvoteCount;
	}

	// TODO this should the database manager handle?
	protected int internalId;

	public PostItem() {
		internalId = globalInt.getAndIncrement();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isNsfw() {
		return nsfw;
	}

	public void setNsfw(boolean nsfw) {
		this.nsfw = nsfw;
	}

	public int getUpvoteCount() {
		return upvoteCount;
	}

	public void setUpvoteCount(int upvoteCount) {
		this.upvoteCount = upvoteCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void addTag(Tag t) {
		tags.add(t);
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getDescriptionHtml() {
		return descriptionHtml;
	}

	public void setDescriptionHtml(String descriptionHtml) {
		this.descriptionHtml = descriptionHtml;
	}

	public boolean isHasLongPostCover() {
		return hasLongPostCover;
	}

	public void setHasLongPostCover(boolean hasLongPostCover) {
		this.hasLongPostCover = hasLongPostCover;
	}

	public int getInternalId() {
		return internalId;
	}

	public void setInternalId(int internalId) {
		this.internalId = internalId;
	}

	/**
	 * @return the promoted
	 */
	public boolean isPromoted() {
		return promoted;
	}

	/**
	 * @param promoted the promoted to set
	 */
	public void setPromoted(boolean promoted) {
		this.promoted = promoted;
	}

	private void setSection(Section section) {
		this.section = section;
	}

	/**
	 * @param postCreationTime the creation time of the post
	 */
	private void setCreationTime(Instant postCreationTime) {
		this.creationTime = postCreationTime;
	}

	/**
	 * @return the section
	 */
	public Section getSection() {
		return section;
	}

	/**
	 * @return the creationTime
	 */
	public Instant getCreationTime() {
		return creationTime;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * @param isVoteMasked if the post is votemasked
	 */
	public void setIsVotedtMasked(boolean isVoteMasked) {
		this.isVoteMasked = isVoteMasked;
	}

	/**
	 * @return the isVoteMasked
	 */
	public boolean isVoteMasked() {
		return isVoteMasked;
	}

	/**
	 * @param isVoteMasked the isVoteMasked to set
	 */
	public void setVoteMasked(boolean isVoteMasked) {
		this.isVoteMasked = isVoteMasked;
	}

	/**
	 * @return the queryTime
	 */
	public ZonedDateTime getQueryTime() {
		return queryTime;
	}

	/**
	 * @param queryTime the queryTime to set
	 */
	public void setQueryTime(ZonedDateTime queryTime) {
		this.queryTime = queryTime;
	}

	/**
	 * Different type of posts
	 * 
	 * @author Kilian
	 *
	 */
	public enum Type {
		Photo, Animated, Article, Video, Gif
	}

	/**
	 * <p>
	 * Overriding classes are only responsible to parse specific fields related to
	 * their content class.
	 * 
	 * @param item The JSON Object containing metainformation to this
	 */
	protected abstract void parse(JSONObject item);

	protected final void parseInternal(JSONObject item) {

		setNsfw(item.getInt("nsfw") != 0);
		setTitle(item.getString("title"));
		setUrl(item.getString("url"));
		setPromoted(item.getInt("promoted") != 0);

		JSONArray tags = item.getJSONArray("tags");
		for (int i = 0; i < tags.length(); i++) {
			JSONObject tagBody = tags.getJSONObject(i);
			addTag(new Tag(tagBody.getString("key"), tagBody.getString("url")));
		}
		setSourceUrl(item.getString("sourceUrl"));
		setCommentCount(item.getInt("commentsCount"));

		setDescriptionHtml(item.getString("descriptionHtml"));
		setId(item.getString("id"));
		setSourceUrl(item.getString("sourceDomain"));
		setUpvoteCount(item.getInt("upVoteCount"));
		setDownvoteCount(item.getInt("downVoteCount"));
		setSection(Section.getSection(item));

		setCreationTime(Instant.ofEpochMilli(item.getLong("creationTs") * 1000));
		setIsVotedtMasked((item.getInt("isVoteMasked") != 0));

		queryTime = ZonedDateTime.now();
	}

	public static PostItem parseItem(JSONObject item) {
		// First create the item by letting subclasses parse

		PostItem pItem = null;
		Type type = Type.valueOf(item.getString("type"));

		switch (type) {
		case Animated:
			pItem = new AnimatedPost();
			break;
		case Article:
			pItem = new ArticlePost();
			System.out.println(item);
			break;
		case Photo:
			pItem = new ImagePost();
			break;
		case Video:
			pItem = new ImagePost(true);
			break;
		default:
			LOGGER.severe("Unrecognized type: " + item.getString("type"));
			break;
		}

		pItem.parse(item);
		pItem.parseInternal(item);
		// TODO is this only for artivles?
		pItem.setHasLongPostCover(item.getInt("hasLongPostCover") != 0);

		return pItem;
	}
}
