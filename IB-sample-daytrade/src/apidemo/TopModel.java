/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import static com.ib.controller.Formats.fmt;
import static com.ib.controller.Formats.fmtPct;
import static com.ib.controller.Formats.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Formats;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewTickType;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.TradingStatus;

class TopModel extends AbstractTableModel {
	private HashMap<String, TopRow> m_map = new HashMap<String, TopRow>();
	private ArrayList<TopRow> m_rows = new ArrayList<TopRow>();

	void addRow(NewContract contract, int position, double avgCost) {
		TopRow full = m_map.get(contract.localSymbol());
		if (full != null) {
			full.updateContract(contract);
			//ApiDemo.INSTANCE.controller().reqTopMktData(contract, "", false, full);
			full.setPosition(position);
			if (full.getStatus() == TradingStatus.buying)  {
				if ( Math.abs(full.getPrePosition()) == Math.abs(position) + full.getNumber()) {
					full.setStatus(TradingStatus.bought);
					ApiDemo.INSTANCE.getDemoLogger().info("bought done");
				}
			} else if (full.getStatus() == TradingStatus.Selling) {
				if ( Math.abs(position) == Math.abs(full.getPrePosition()) + full.getNumber()) {
					full.setStatus(TradingStatus.sold);
					ApiDemo.INSTANCE.getDemoLogger().info("sold done");
				}
			}
			fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
		} else {
			TopRow row = new TopRow(this, contract, position, avgCost);
			m_rows.add(row);
			ApiDemo.INSTANCE.getDemoLogger().info( "requested market data for:" + contract.description());
			ApiDemo.INSTANCE.controller().reqTopMktData(contract, "", false, row);
			m_map.put(contract.localSymbol(), row);
			fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
		}
	}

	public ArrayList<TopRow> getRowsList() {
		return m_rows;
	}

	void addRow(TopRow row) {
		if (m_rows.contains(row)) {
			return;
		}

		m_rows.add(row);
		fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
	}

	public void desubscribe() {
		for (TopRow row : m_rows) {
			ApiDemo.INSTANCE.controller().cancelTopMktData(row);
		}
	}

	@Override
	public int getRowCount() {
		return m_rows.size();
	}

	@Override
	public int getColumnCount() {
		return 18;
	}

	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Description";
		case 1:
			return "Bid Size";
		case 2:
			return "Bid";
		case 3:
			return "Ask";
		case 4:
			return "Ask Size";
		case 5:
			return "Last";
		case 6:
			return "Time";
		case 7:
			return "Change";
		case 8:
			return "Volume";
		case 9:
			return "stop price"; // jicheng
		case 10:
			return "Position";
		case 11:
			return "AvgCost"; // jicheng
		case 12:
			return "PreviousPosition"; // jicheng
		case 13:
			return "TradingCount";
		case 14:
			return "TradingStatus";
		case 15:
			return "Number";
		case 16:
			return "Max";
		case 17:
			return "Min";
		default:
			return null;
		}
	}

	@Override
	public Object getValueAt(int rowIn, int col) {
		TopRow row = m_rows.get(rowIn);
		switch (col) {
		case 0:
			return row.m_description;
		case 1:
			return row.m_bidSize;
		case 2:
			return fmt(row.m_bid);
		case 3:
			return fmt(row.m_ask);
		case 4:
			return row.m_askSize;
		case 5:
			return fmt(row.m_last);
		case 6:
			return fmtTime(row.m_lastTime);
		case 7:
			return row.change();
		case 8:
			return Formats.fmt0(row.m_volume);
		case 9:
			return fmt(row.m_stop_price); // jicheng
		case 10:
			return row.m_position;
		case 11:
			return fmt(row.m_avgCost); // jicheng
		case 12:
			return row.m_prePosition;
		case 13:
			return row.m_tradingCount; // jicheng
		case 14:
			return row.m_status.toString(); // jicheng
		case 15:
			return fmt(row.m_number); // jicheng
		case 16:
			return fmt(row.m_max); // jicheng
		case 17:
			return fmt(row.m_min); // jicheng
		default:
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int rowSet, int col) {
		if (col == 9 || col == 15 || col == 16  || col == 17) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setValueAt(Object value, int rowSet, int col) {
		TopRow row = m_rows.get(rowSet);

		switch (col) {
		case 9:
			row.m_stop_price = new Double(value.toString());
			fireTableDataChanged();
			break;
		case 15:
			row.m_number = new Double(value.toString());
			fireTableDataChanged();
			break;
		case 16:
			row.m_max = new Double(value.toString());
			fireTableDataChanged();
			break;
		case 17:
			row.m_min = new Double(value.toString());
			fireTableDataChanged();
			break;
		}
		
	}

	public void color(TableCellRenderer rend, int rowIn, Color def) {
		TopRow row = m_rows.get(rowIn);
		Color c = row.m_frozen ? Color.gray : def;
		((JLabel) rend).setForeground(c);
	}

	public void cancel(int i) {
		ApiDemo.INSTANCE.controller().cancelTopMktData(m_rows.get(i));
	}

	static class TopRow extends TopMktDataAdapter {
		AbstractTableModel m_model;
		String m_description;
		double m_bid;
		double m_ask;
		double m_last;
		long m_lastTime;
		int m_bidSize;
		int m_askSize;
		double m_close;
		int m_volume;
		boolean m_frozen;
		double m_stop_price; // jicheng
		double m_number; // jicheng
		double m_max; // jicheng
		double m_min; // jicheng
		NewContract m_contract;
		NewOrder m_order;
		int m_position;
		int m_prePosition;
		double m_avgCost;
		int m_tradingCount;
		TradingStatus m_status;

		TopRow(AbstractTableModel model, NewContract contract, int position, double avgCost) {
			m_model = model;
			m_contract = contract;
			m_description = contract.description();
			m_stop_price = -1; // jicheng
			m_number = 0; // jicheng
			m_max = 0; // jicheng
			m_min = 0; // jicheng
			m_position = position;
			m_avgCost = avgCost;
			m_tradingCount = 0;
			m_status = TradingStatus.None;
			m_prePosition = -1;
		}

		public synchronized int getPrePosition() {
			return m_prePosition;
		}

		public synchronized void setPrePosition(int prePosition) {
			m_prePosition = prePosition;
		}

		public synchronized TradingStatus getStatus() {
			return m_status;
		}

		public synchronized void setStatus(TradingStatus status) {
			m_status = status;
		}

		public synchronized int getCount() {
			return m_tradingCount;
		}

		public synchronized void setCount(int count) {
			m_tradingCount = count;
		}

		public double getBidPrice() {
			return m_bid;
		}

		public double getAskPrice() {
			return m_ask;
		}

		public double getStopPrice() {
			return m_stop_price;
		}

		public double getNumber() {
			return m_number;
		}

		public double getMax() {
			return m_max;
		}

		public double getMin() {
			return m_min;
		}

		public NewContract getContract() {
			return m_contract;
		}

		public int getPosition() {
			return m_position;
		}

		public synchronized void setPosition(int position) {
			m_position = position;
		}

		public synchronized void updateContract(NewContract contract) {
			m_contract = contract;
		}

		public String change() {
			return m_close == 0 ? null : fmtPct((m_last - m_close) / m_close);
		}

		@Override
		public synchronized void tickPrice(NewTickType tickType, double price, int canAutoExecute) {
			switch (tickType) {
			case BID:
				m_bid = price;
				break;
			case ASK:
				m_ask = price;
				break;
			case LAST:
				m_last = price;
				break;
			case CLOSE:
				m_close = price;
				break;
			}
			m_model.fireTableDataChanged(); // should use a timer to be more
											// efficient

		}

		@Override
		public synchronized void tickSize(NewTickType tickType, int size) {
			switch (tickType) {
			case BID_SIZE:
				m_bidSize = size;
				break;
			case ASK_SIZE:
				m_askSize = size;
				break;
			case VOLUME:
				m_volume = size;
				break;
			}
			m_model.fireTableDataChanged();
		}

		@Override
		public synchronized void tickString(NewTickType tickType, String value) {
			switch (tickType) {
			case LAST_TIMESTAMP:
				m_lastTime = Long.parseLong(value) * 1000;
				break;
			}
		}

		@Override
		public void marketDataType(MktDataType marketDataType) {
			m_frozen = marketDataType == MktDataType.Frozen;
			m_model.fireTableDataChanged();
		}

		public String getdescription() {
			// TODO Auto-generated method stub
			return m_description;
		}
	}

}
