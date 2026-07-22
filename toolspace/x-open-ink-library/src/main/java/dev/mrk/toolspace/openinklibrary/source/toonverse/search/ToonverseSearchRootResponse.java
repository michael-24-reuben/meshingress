package dev.mrk.toolspace.openinklibrary.source.toonverse.search;

public class ToonverseSearchRootResponse {
	private String source;
	private ToonverseSearchDataNode data;

	public ToonverseSearchRootResponse(String source, ToonverseSearchDataNode data) {
		this.source = source;
		this.data = data;
	}

	public String getSource() {
		return this.source;
	}

	public ToonverseSearchDataNode getData() {
		return this.data;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setData(ToonverseSearchDataNode data) {
		this.data = data;
	}
}

