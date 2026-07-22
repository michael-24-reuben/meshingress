package dev.mrk.toolspace.openinklibrary.source.toonverse.series;

public class ToonverseSeriesRootResponse {
	private Boolean success;
	private ToonverseSeriesDataNode data;

	public ToonverseSeriesRootResponse(Boolean success, ToonverseSeriesDataNode data) {
		this.success = success;
		this.data = data;
	}

	public Boolean getSuccess() {
		return this.success;
	}

	public ToonverseSeriesDataNode getData() {
		return this.data;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public void setData(ToonverseSeriesDataNode data) {
		this.data = data;
	}
}

