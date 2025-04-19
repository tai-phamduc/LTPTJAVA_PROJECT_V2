package gui.application.form.other.station;

import java.util.HashMap;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import entity.Station;
import utils.ServerFetcher;

public class StationTableModel extends AbstractTableModel {

	private List<Station> stations;
	private String[] columnNames = { "Mã ga", "Tên ga" };

	public StationTableModel() {
		HashMap<String, String> payload = new HashMap<>();
		stations = (List<Station>) ServerFetcher.fetch("station", "getAllStation", payload);
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public int getRowCount() {
		return stations.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Station station = stations.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return station.getStationID();
		case 1:
			return station.getStationName();
		}
		return null;
	}

	public void setTrainDetailsList(List<Station> stations) {
		this.stations = stations;
	}

}