/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import static com.ib.controller.Formats.fmt;
import static com.ib.controller.Formats.fmtPct;
import static com.ib.controller.Formats.*;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.ib.controller.ApiController.IOptHandler;
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
			fireTableDataChanged();
		} else {
			TopRow row = new TopRow(this, contract, position, avgCost);
			m_rows.add(row);
			ApiDemo.INSTANCE.getDemoLogger().info( "requested market data for:" + contract.description());
			//ApiDemo.INSTANCE.controller().reqTopMktData(contract, "", false, row);
			ApiDemo.INSTANCE.controller().reqOptionMktData(contract, "", false, row);
			m_map.put(contract.localSymbol(), row);
			fireTableDataChanged();//fireTableRowsInserted(m_rows.size() - 1, m_rows.size() - 1);
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
		return 30;
	}

	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Description";
		case 1:
			return "BidSize";
		case 2:
			return "Bid";
		case 3:
			return "Ask";
		case 4:
			return "AskSize";
		case 5:
			return "Last";
		case 6:
			return "Time";
		case 7:
			return "Change";
		case 8:
			return "Volume";
		case 9:
			return "TradingLimit"; // jicheng
		case 10:
			return "Position";
		case 11:
			return "AvgCost"; // jicheng
		case 12:
			return "PrePosition"; // jicheng
		case 13:
			return "T-Count";
		case 14:
			return "T-Status";
		case 15:
			return "Number";
		case 16:
			return "Max";
		case 17:
			return "Min";
		case 18:
			return "impVol";
		case 19:
			return "delta";
		case 20:
			return "gamma";
		case 21:
			return "vega";
		case 22:
			return "theta";
		case 23:
			return "undPrice";
		case 24:
			return "optPrice";
		case 25:
			return "impVol-S";
		case 26:
			return "undPrice-S";
		case 27:
			return "optPrice-S";
		case 28: 
			return "Start";
		case 29: 
			return "End";			
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
			return row.m_tradinglimit; // jicheng
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
		case 18:
			return fmt(row.m_impVol); // jicheng
		case 19:
			return fmt(row.m_delta); // jicheng
		case 20:
			return fmt(row.m_gamma); // jicheng
		case 21:
			return fmt(row.m_vega); // jicheng
		case 22:
			return fmt(row.m_theta); // jicheng
		case 23:
			return fmt(row.m_undPrice); // jicheng
		case 24:
			return fmt(row.m_optPrice); // jicheng
		case 25:
			return fmt(row.m_impVol_s); // jicheng
		case 26:
			return fmt(row.m_undPrice_s); // jicheng
		case 27:
			return fmt(row.m_optPrice_s); // jicheng
		case 28: 
			return (new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")).format(row.m_cal_start.getTime());				
		case 29: 
			return (new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")).format(row.m_cal_end.getTime());
		
		default:
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int rowSet, int col) {
		if (col == 9 || col == 12 || col == 14 || col == 15 || col == 16  || col == 17 
		 || col == 25 || col == 26 || col == 28 || col == 29) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setValueAt(Object value, int rowSet, int col) {
		TopRow row = m_rows.get(rowSet);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

		switch (col) {
		case 9:
			row.m_tradinglimit = new Integer(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("trading limit: " + row.m_tradinglimit);
			break;
		case 12:
			row.m_prePosition = new Integer(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("prePosition changed to: " + value.toString());
			fireTableDataChanged();
			break;
		case 14:
			if (value.toString().equals("None")) {
				row.setStatus(TradingStatus.None);
			} else if (value.toString().equals("Selling")) {
				row.setStatus(TradingStatus.Selling);
			} else if (value.toString().equals("sold")) {
				row.setStatus(TradingStatus.sold);
			} else if (value.toString().equals("buying")) {
				row.setStatus(TradingStatus.buying);
			} else if (value.toString().equals("bought")) {
				row.setStatus(TradingStatus.bought);
			}
			ApiDemo.INSTANCE.getDemoLogger().info("status changed to: " + value.toString());
			fireTableDataChanged();
			break;
		case 15:
			row.m_number = new Double(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("trading unit: " + value.toString());
			fireTableDataChanged();
			break;
		case 16:
			row.m_max = new Double(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("Max value: " + value.toString());
			fireTableDataChanged();
			break;
		case 17:
			row.m_min = new Double(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("Min value: " + value.toString());
			fireTableDataChanged();
			break;
		case 25:
			row.m_impVol_s = new Double(value.toString());
			if ( row.m_impVol_s > 0 ) {
				ApiDemo.INSTANCE.controller().cancelOptionComp(row);
				ApiDemo.INSTANCE.controller().reqOptionComputation(row.getContract(), row.m_impVol_s, row.m_undPrice_s, row);
				ApiDemo.INSTANCE.getDemoLogger().info("Set impVol: " + value.toString());
			}
			fireTableDataChanged();
			break;
		case 26:
			row.m_undPrice_s = new Double(value.toString());
			if ( row.m_undPrice_s > 0) { 
				ApiDemo.INSTANCE.controller().cancelOptionComp(row);
				ApiDemo.INSTANCE.controller().reqOptionComputation(row.getContract(), row.m_impVol_s, row.m_undPrice_s, row);
				ApiDemo.INSTANCE.getDemoLogger().info("Set undPrice: " + value.toString());
			}
			fireTableDataChanged();
			break;
		case 28:
			try {
				row.m_cal_start.setTime(sdf.parse(value.toString()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ApiDemo.INSTANCE.getDemoLogger().info("start: " + value.toString());
			break;
		case 29:
			try {
				row.m_cal_end.setTime(sdf.parse(value.toString()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ApiDemo.INSTANCE.getDemoLogger().info("end: " + value.toString());	
			break;
		}
		
		fireTableDataChanged();
	}

	public void color(TableCellRenderer rend, int rowIn, Color def) {
		TopRow row = m_rows.get(rowIn);
		Color c = row.m_frozen ? Color.gray : def;
		((JLabel) rend).setForeground(c);
	}

	public void cancel(int i) {
		ApiDemo.INSTANCE.controller().cancelTopMktData(m_rows.get(i));
	}

	static class TopRow extends TopMktDataAdapter implements IOptHandler {
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
		int m_tradinglimit;
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
		double m_impVol;
		double m_delta;
		double m_gamma;
		double m_vega;
		double m_theta;
		double m_undPrice;
		double m_optPrice;
		double m_impVol_s;
		double m_undPrice_s;
		double m_optPrice_s;
		Calendar m_cal_start = Calendar.getInstance(); 
		Calendar m_cal_end = Calendar.getInstance();


		TopRow(AbstractTableModel model, NewContract contract, int position, double avgCost) {
			m_model = model;
			m_contract = contract;
			m_description = contract.description();
			m_number = 0; // jicheng
			m_max = 0; // jicheng
			m_min = 0; // jicheng
			m_position = position;
			m_avgCost = avgCost;
			m_tradingCount = 0;
			m_status = TradingStatus.None;
			m_prePosition = 9999;
			m_impVol = -1;
			m_impVol_s = -1;
			m_undPrice_s = -1;
			m_optPrice_s = -1;
			m_tradinglimit = 5;
			
			m_cal_start.set(Calendar.DAY_OF_MONTH, m_cal_start.get(Calendar.DAY_OF_MONTH)-1);
			m_cal_start.set(Calendar.HOUR_OF_DAY, 17);
			m_cal_start.set(Calendar.MINUTE, 01);
			m_cal_start.set(Calendar.SECOND , 00);
				
			m_cal_end.set(Calendar.HOUR_OF_DAY, 15);
			m_cal_end.set(Calendar.MINUTE, 14);
			m_cal_end.set(Calendar.SECOND , 00);
		}

		public  synchronized Calendar getStart() {
			return m_cal_start;
		}

		public  synchronized Calendar getEnd() {
			return m_cal_end;
		}

		public  synchronized int getTradinglimit() {
			return m_tradinglimit;
		}

		public  synchronized void setTradinglimit( int number) {
			 m_tradinglimit = number;
		}

		public  synchronized int getPrePosition() {
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


		public double getNumber() {
			return m_number;
		}

		public double getMax() {
			return m_max;
		}

		public double getMin() {
			return m_min;
		}

		public synchronized NewContract getContract() {
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
		public void tickOptionComputation( NewTickType tickType, double impVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
			if (tickType == NewTickType.MODEL_OPTION) {
				m_impVol = impVol;
				m_delta = delta;
				m_gamma = gamma;
				m_vega = vega;
				m_theta = theta;
				m_optPrice = optPrice;
				m_undPrice = undPrice;
			} else if ( tickType == NewTickType.CUST_OPTION_COMPUTATION) {
				m_optPrice_s = optPrice;
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
