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
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.ApiController.TopMktDataAdapter;
import com.ib.controller.Bar;
import com.ib.controller.Formats;
import com.ib.controller.NewContract;
import com.ib.controller.NewOrder;
import com.ib.controller.NewTickType;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.TradingStatus;
import com.ib.controller.Types.WhatToShow;

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
			ApiDemo.INSTANCE.controller().reqRealTimeBars(contract, WhatToShow.MIDPOINT, false, row);
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
		return 35;
	}

	@Override
	public String getColumnName(int col) {
		switch (col) {
		case 0:
			return "Description";
		case 1:
			return "BidSize";
		case 2:
			return "AskSize";
		case 3:
			return "Last";
		case 4:
			return "Time";
		case 5:
			return "Change";
		case 6:
			return "MaxTail";
		case 7:
			return "MinTail"; // jicheng
		case 8:
			return "Bid";
		case 9:
			return "Ask";
		case 10:
			return "5sAvg"; // jicheng
		case 11:
			return "Position";
		case 12:
			return "PrePosition"; // jicheng
		case 13:
			return "BOXCount";
		case 14:
			return "LMTCount";
		case 15:
			return "BOXLimit"; // jicheng
		case 16:
			return "LMTLimit";
		case 17:
			return "Unit";
		case 18:
			return "T-Status";
		case 19:
			return "Max";
		case 20:
			return "Min";
		case 21:
			return "LMT";
		case 22:
			return "OFFSET";
		case 23:
			return "ImpVol";
		case 24:
			return "Delta";
		case 25:
			return "Gamma";
		case 26:
			return "Vega";
		case 27:
			return "Theta";
		case 28:
			return "UndPrice";
		case 29:
			return "OptPrice";
		case 30:
			return "ImpVol-S";
		case 31:
			return "UndPrice-S";
		case 32:
			return "OptPrice-S";
		case 33: 
			return "Start";
		case 34: 
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
			return row.m_askSize;
		case 3:
			return fmt(row.m_last);
		case 4:
			return fmtTime(row.m_lastTime);
		case 5:
			return row.change();
		case 6:
			return fmt(row.m_maxTail);
		case 7:
			return fmt(row.m_minTail); // jicheng
		case 8:
			return fmt(row.m_bid);
		case 9:
			return fmt(row.m_ask);
		case 10:
			return fmt(row.m_5sAvg.close()); // jicheng
		case 11:
			return row.m_position;
		case 12:
			return row.m_prePosition;
		case 13:
			return row.m_boxTradingCounter; // jicheng
		case 14:
			return row.m_lmtTradingCounter; // jicheng
		case 15:
			return row.m_boxtradinglimit;
		case 16:
			return row.m_stoptradinglimit;
		case 17:
			return row.m_unit; // jicheng
		case 18:
			return row.m_status.toString(); // jicheng
		case 19:
			return fmt(row.m_max); // jicheng
		case 20:
			return fmt(row.m_min); // jicheng
		case 21:
			return fmt(row.m_lmt); // jicheng
		case 22:
			return fmt(row.m_offset); // jicheng
		case 23:
			return fmt(row.m_impVol); // jicheng
		case 24:
			return fmt(row.m_delta); // jicheng
		case 25:
			return fmt(row.m_gamma); // jicheng
		case 26:
			return fmt(row.m_vega); // jicheng
		case 27:
			return fmt(row.m_theta); // jicheng
		case 28:
			return fmt(row.m_undPrice); // jicheng
		case 29:
			return fmt(row.m_optPrice); // jicheng
		case 30:
			return fmt(row.m_impVol_s); // jicheng
		case 31:
			return fmt(row.m_undPrice_s); // jicheng
		case 32:
			return fmt(row.m_optPrice_s); // jicheng
		case 33: 
			return (new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")).format(row.m_cal_start.getTime());				
		case 34: 
			return (new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")).format(row.m_cal_end.getTime());
		
		default:
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int rowSet, int col) {
		if (col == 6 || col == 7 || col == 12 || col == 15 || col == 16 || col == 17 || col == 18  || col == 19 
		 || col == 20 || col == 21 || col == 22 || col == 30 || col == 31 || col == 33 || col == 34) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setValueAt(Object value, int rowSet, int col) {
		TopRow row = m_rows.get(rowSet);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		Double tmp_double = 0.0;
		Integer    tmp_int = 0;

		switch (col) {
		case 6:
			tmp_double = new Double(value.toString() ); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else if (row.m_status==TradingStatus.S_M_M && tmp_double > row.getBidPrice() ) {
				ApiDemo.INSTANCE.getDemoLogger().info("getBidPrice() < maxtail");
				break;
			} else {
				row.m_maxTail = tmp_double;
				ApiDemo.INSTANCE.getDemoLogger().info("MaxTail: " + row.m_maxTail + " " + row.getContract().description());
				break;
			}
		case 7:
			tmp_double = new Double(value.toString() ); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else if ( row.m_status==TradingStatus.B_M_M &&  tmp_double < row.getAskPrice()   ) {
				ApiDemo.INSTANCE.getDemoLogger().info("row.getAskPrice() > minTail");
				break;
			} else {
				row.m_minTail = tmp_double;
				ApiDemo.INSTANCE.getDemoLogger().info("MinTail: " + row.m_minTail + " " + row.getContract().description());
				break;
			}
		case 12:
			row.m_prePosition = new Integer(value.toString());
			ApiDemo.INSTANCE.getDemoLogger().info("PrePosition: " + row.m_prePosition + " " + row.getContract().description());
			break;
		case 15:
			tmp_int = new Integer(value.toString()); 
			if ( tmp_int >= 0 ) {
				row.m_boxtradinglimit = tmp_int;
				ApiDemo.INSTANCE.getDemoLogger().info("BoxTradinglimit: " + tmp_int + " " + row.getContract().description());
				fireTableDataChanged();
			}	
			break;
		case 16:
			tmp_int = new Integer(value.toString()); 
			if ( tmp_int >= 0 ) {
				row.m_stoptradinglimit = tmp_int;
				ApiDemo.INSTANCE.getDemoLogger().info("StopTradinglimit: " + tmp_int + " " + row.getContract().description());
				fireTableDataChanged();
			}	
			break;
		case 17:
			tmp_int = new Integer(value.toString()); 
			if ( (row.getPosition() + tmp_int) <= 1 && (tmp_int >= 0) ) {
				row.m_unit = tmp_int;
				ApiDemo.INSTANCE.getDemoLogger().info("trading unit: " + tmp_int);
				fireTableDataChanged();
			} else {
				ApiDemo.INSTANCE.getDemoLogger().info("trading unit larger than short position " + " " + row.getContract().description());
			}
			break;
		case 18:
			if (value.toString().equals("init")) {
				row.m_unit = 0; 
				row.m_max = 0; 
				row.m_min = 0; 
				row.m_boxTradingCounter = 0;
				row.m_lmtTradingCounter = 0;
				row.m_prePosition = 9999;
				row.m_boxtradinglimit = 35;
				row.m_stoptradinglimit = 7;
				row.m_lmt = 0;
				row.m_offset = 0;
				row.m_maxTail = 0;
				row.m_minTail = 0;
				
				if ( row.m_status==TradingStatus.Stop) {
					ApiDemo.INSTANCE.controller().reqOptionMktData(row.getContract(), "", false, row);
					ApiDemo.INSTANCE.getDemoLogger().info("start market data flow" + " " + row.getContract().description());
				}
				
				row.setStatus(TradingStatus.Init);
			} else if (value.toString().equals("stop")) {
				row.setStatus(TradingStatus.Stop);
				ApiDemo.INSTANCE.controller().cancelOptionMktData(row);
				ApiDemo.INSTANCE.getDemoLogger().info("stop market data flow" + " " + row.getContract().description());
			} else if (value.toString().equals("sm")) {
				row.setStatus(TradingStatus.S_M);
			} /*else if (value.toString().equals("Buying")) {
				row.setStatus(TradingStatus.Buying);
			} */else if (value.toString().equals("bm")) {
				row.setStatus(TradingStatus.B_M);
			} else if (value.toString().equals("blo")) {
				row.setStatus(TradingStatus.B_L_O);
			} else if (value.toString().equals("bl")) {
				row.setStatus(TradingStatus.B_L);
			} else if (value.toString().equals("smm")) {
				row.setStatus(TradingStatus.S_M_M);
			} else if (value.toString().equals("bmm")) {
				row.setStatus(TradingStatus.B_M_M);
			}
			ApiDemo.INSTANCE.getDemoLogger().info("status: " + value.toString() + " " + row.getContract().description());
			fireTableDataChanged();
			break;
		case 19:
			tmp_double = new Double(value.toString() ); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else	if (row.m_status==TradingStatus.Init || row.m_status==TradingStatus.B_M) {
				if ( (tmp_double >= 0.0) && (tmp_double > row.m_bid) && (tmp_double > row.m_min) && (tmp_double < row.m_lmt) ) {
					row.m_max = tmp_double;
					row.setMaxtail(row.m_max - row.getOffset());
					ApiDemo.INSTANCE.getDemoLogger().info("Max: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}  else if (row.m_status==TradingStatus.S_M || row.m_status==TradingStatus.B_L || row.m_status==TradingStatus.B_L_O || row.m_status==TradingStatus.S_M_M) {
				if ( (tmp_double >= 0.0)  && (tmp_double > row.m_min) && (tmp_double < row.m_lmt) ) {
					row.m_max = tmp_double;
					row.setMaxtail(row.m_max - row.getOffset());
					ApiDemo.INSTANCE.getDemoLogger().info("Max: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}	
			break;
		case 20:
			tmp_double = new Double(value.toString()); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else	if (row.m_status==TradingStatus.S_M) {
				if ( (tmp_double >= 0.0) && (tmp_double < row.m_ask) && (tmp_double < row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_min = tmp_double;
					row.setMinTail(row.getMin() + row.getOffset());
					ApiDemo.INSTANCE.getDemoLogger().info("Min: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}  else  {
				if ( (tmp_double >= 0.0) && (tmp_double < row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_min = tmp_double;
					row.setMinTail(row.getMin() + row.getOffset());
					ApiDemo.INSTANCE.getDemoLogger().info("Min: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}	
			break;
		case 21:
			tmp_double = new Double(value.toString()); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else	if (row.m_status==TradingStatus.Init || row.m_status==TradingStatus.B_M || row.m_status==TradingStatus.S_M_M) {
				if ( (tmp_double >= 0.0)  && (tmp_double > row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_lmt = tmp_double;
					ApiDemo.INSTANCE.getDemoLogger().info("m_lmt: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}  else if (row.m_status==TradingStatus.S_M  ) {
				if ( (tmp_double >= 0.0) && ( tmp_double > row.m_5sAvg.close() ) && (tmp_double > row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_lmt = tmp_double;
					ApiDemo.INSTANCE.getDemoLogger().info("m_lmt: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}  else if (row.m_status==TradingStatus.B_L ) {
				if ( (tmp_double >= 0.0) && ( tmp_double+row.m_offset > row.m_5sAvg.close() ) && (tmp_double > row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_lmt = tmp_double;
					ApiDemo.INSTANCE.getDemoLogger().info("m_lmt: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			} else if (row.m_status==TradingStatus.B_L_O) {
				if ( (tmp_double >= 0.0) && ( tmp_double < row.m_5sAvg.close() ) && (tmp_double > row.m_max) /*&& row.m_status == TradingStatus.Init*/) {
					row.m_lmt = tmp_double;
					ApiDemo.INSTANCE.getDemoLogger().info("m_lmt: " + tmp_double + " " + row.getContract().description());
					fireTableDataChanged();
				}
			}	
			break;
		case 22:
			tmp_double = new Double(value.toString()); 
			if (((int)(tmp_double*100))%25 != 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("must be multiple by 0.25");
				break;
			} else	if (row.m_status==TradingStatus.B_L ) {			
				if ( tmp_double >= 0.0 && ( tmp_double+row.m_lmt > row.m_5sAvg.close() ) ) {
				row.m_offset = tmp_double;
				ApiDemo.INSTANCE.getDemoLogger().info("offset: " + tmp_double + " " + row.getContract().description());
				fireTableDataChanged();
				}
			} else {
				if ( tmp_double >= 0.0  ) {
				row.m_offset = tmp_double;
				ApiDemo.INSTANCE.getDemoLogger().info("offset: " + tmp_double + " " + row.getContract().description());
				fireTableDataChanged();
				}
			}
			break;
		case 30:
			row.m_impVol_s = new Double(value.toString());
			if ( row.m_impVol_s > 0 ) {
				ApiDemo.INSTANCE.controller().cancelOptionComp(row);
				ApiDemo.INSTANCE.controller().reqOptionComputation(row.getContract(), row.m_impVol_s, row.m_undPrice_s, row);
				ApiDemo.INSTANCE.getDemoLogger().info("Set impVol: " + value.toString() + " " + row.getContract().description());
			}
			fireTableDataChanged();
			break;
		case 31:
			row.m_undPrice_s = new Double(value.toString());
			if ( row.m_undPrice_s > 0) { 
				ApiDemo.INSTANCE.controller().cancelOptionComp(row);
				ApiDemo.INSTANCE.controller().reqOptionComputation(row.getContract(), row.m_impVol_s, row.m_undPrice_s, row);
				ApiDemo.INSTANCE.getDemoLogger().info("Set undPrice: " + value.toString() + " " + row.getContract().description());
			}
			fireTableDataChanged();
			break;
		case 33:
			try {
				row.m_cal_start.setTime(sdf.parse(value.toString()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ApiDemo.INSTANCE.getDemoLogger().info("start: " + value.toString() + " " + row.getContract().description());
			break;
		case 34:
			try {
				row.m_cal_end.setTime(sdf.parse(value.toString()));
				row.m_lastprint = 1;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ApiDemo.INSTANCE.getDemoLogger().info("end: " + value.toString() + " " + row.getContract().description());	
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

	static class TopRow extends TopMktDataAdapter implements IOptHandler, IRealTimeBarHandler {
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
		int m_boxtradinglimit;
		int m_stoptradinglimit;
		int m_unit; // jicheng
		double m_max; // jicheng
		double m_min; // jicheng
		NewContract m_contract;
		NewOrder m_order;
		int m_position;
		int m_prePosition;
		double m_avgCost;
		int m_boxTradingCounter;
		int m_lmtTradingCounter;
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
		double m_lmt;
		double m_maxTail;
		double m_minTail;
		double m_offset;
		int	   m_counter_pricelog;
		int    m_lastprint;
		Bar    m_5sAvg;
		Calendar m_cal_start = Calendar.getInstance(); 
		Calendar m_cal_end = Calendar.getInstance();


		TopRow(AbstractTableModel model, NewContract contract, int position, double avgCost) {
			m_model = model;
			m_contract = contract;
			m_description = contract.description();
			m_unit = 0; // jicheng
			m_max = 0; // jicheng
			m_min = 0; // jicheng
			m_position = position;
			m_avgCost = avgCost;
			m_boxTradingCounter = 0;
			m_lmtTradingCounter = 0;
			m_status = TradingStatus.Init;
			m_prePosition = 9999;
			m_impVol = -1;
			m_impVol_s = -1;
			m_undPrice_s = -1;
			m_optPrice_s = -1;
			m_boxtradinglimit = 35;
			m_stoptradinglimit = 7;
			m_lmt = 0;
			m_offset = 0;
			m_maxTail = 0;
			m_minTail = 0;
			m_lastprint = 1;
			m_5sAvg = new Bar(0, 0, 0, 0, 0, 0, 0, 0);
			m_counter_pricelog = 0;
			
			
			m_cal_start.set(Calendar.DAY_OF_MONTH, m_cal_start.get(Calendar.DAY_OF_MONTH)-1);
			m_cal_start.set(Calendar.HOUR_OF_DAY, 17);
			m_cal_start.set(Calendar.MINUTE, 01);
			m_cal_start.set(Calendar.SECOND , 00);
				
			m_cal_end.set(Calendar.HOUR_OF_DAY, 15);
			m_cal_end.set(Calendar.MINUTE, 14);
			m_cal_end.set(Calendar.SECOND , 55);
		}

		@Override public void realtimeBar(Bar bar) {
			m_5sAvg = bar;
		}

		public Bar get5sAvg () {
			return m_5sAvg;
		}
		
		public void clearLastprint () {
			m_lastprint = 0;
		}
		public void closingPrint () {
			if ( m_lastprint == 1 ) {
				ApiDemo.INSTANCE.getDemoLogger().info("EoD Record: "+m_description + "-bid:" + m_bid + "-ask:" + m_ask + "-5s:" + m_5sAvg.close() + "-MaxTail: " + m_maxTail + "MinTail: " + m_minTail);
				ApiDemo.INSTANCE.getDemoLogger().info("-Position:"+m_position + "-PrePosition:" + m_prePosition + "-BoxTradingLimit:" + m_boxtradinglimit + "-StopTradingLimit:" + m_stoptradinglimit);
				ApiDemo.INSTANCE.getDemoLogger().info("-boxTradingCounter:" + m_boxTradingCounter + "-lmtTradingCounter:"+m_lmtTradingCounter + "-Unit:" + m_unit + "-status:" + m_status.toString());
				ApiDemo.INSTANCE.getDemoLogger().info("-Max:"+m_max + "-Min:" + m_min + "-Limit:" + m_lmt + "-Offset:" + m_offset);
			} 
		}
		public  synchronized Calendar getStart() {
			return m_cal_start;
		}

		public  synchronized Calendar getEnd() {
			return m_cal_end;
		}

		public  synchronized int getBoxTradinglimit() {
			return m_boxtradinglimit;
		}

		public  synchronized void setBoxTradinglimit( int number) {
			 m_boxtradinglimit = number;
		}

		public  synchronized int getStopTradinglimit() {
			return m_stoptradinglimit;
		}

		public  synchronized void setStopTradinglimit( int number) {
			m_stoptradinglimit = number;
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

		public synchronized int getBoxTradingCounter() {
			return m_boxTradingCounter;
		}

		public synchronized void setBoxTradingCounter(int number) {
			m_boxTradingCounter = number;
		}
		
		public synchronized int getlmtTradingCounter() {
			return m_lmtTradingCounter;
		}

		public synchronized void setlmtTradingCounter(int number) {
			m_lmtTradingCounter = number;
		}

		public double getMaxTail() {
			return m_maxTail;
		}

		public void setMaxtail(double num) {
			m_maxTail = num;
		}

		public double getMintail() {
			return m_minTail;
		}

		public void setMinTail(double num) {
			m_minTail = num;
		}

		public double getBidPrice() {
			return m_bid;
		}

		public double getAskPrice() {
			return m_ask;
		}

		public int getUnit() {
			return m_unit;
		}

		public double getMax() {
			return m_max;
		}

		public double getMin() {
			return m_min;
		}

		public double getLmt() {
			return m_lmt;
		}

		public void setLmt(double limit) {
			m_lmt = limit;
		}

		public double getOffset() {
			return m_offset;
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
