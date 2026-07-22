package dev.mrk.toolspace.openinklibrary.source.toonverse.chapter;

public class ToonverseChapterRootResponse {
	private Boolean success;
	private ToonverseChapterDataNode data;

	public ToonverseChapterRootResponse(Boolean success, ToonverseChapterDataNode data) {
		this.success = success;
		this.data = data;
	}

	public Boolean getSuccess() {
		return this.success;
	}

	public ToonverseChapterDataNode getData() {
		return this.data;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public void setData(ToonverseChapterDataNode data) {
		this.data = data;
	}
}

