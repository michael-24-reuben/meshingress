package dev.mrk.toolspace.openinklibrary.source.toonverse.chapter;

public class ToonverseChapterPageNode {
	private String id;
	private String chapterId;
	private Long number;
	private String imageUrl;
	private String placeholder;
	private Long width;
	private Long height;
	private Boolean hidden;
	private Object panelMotionData; // unknown type: results not concluded

	public ToonverseChapterPageNode(String id, String chapterId, Long number, String imageUrl, String placeholder, Long width, Long height, Boolean hidden, Object panelMotionData) {
		this.id = id;
		this.chapterId = chapterId;
		this.number = number;
		this.imageUrl = imageUrl;
		this.placeholder = placeholder;
		this.width = width;
		this.height = height;
		this.hidden = hidden;
		this.panelMotionData = panelMotionData;
	}

	public String getId() {
		return this.id;
	}

	public String getChapterId() {
		return this.chapterId;
	}

	public Long getNumber() {
		return this.number;
	}

	public String getImageUrl() {
		return this.imageUrl;
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public Long getWidth() {
		return this.width;
	}

	public Long getHeight() {
		return this.height;
	}

	public Boolean getHidden() {
		return this.hidden;
	}

	public Object getPanelMotionData() {
		return this.panelMotionData;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setChapterId(String chapterId) {
		this.chapterId = chapterId;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}

	public void setWidth(Long width) {
		this.width = width;
	}

	public void setHeight(Long height) {
		this.height = height;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public void setPanelMotionData(Object panelMotionData) {
		this.panelMotionData = panelMotionData;
	}
}
