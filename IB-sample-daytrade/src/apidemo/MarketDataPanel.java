/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.Timer;

import apidemo.TopModel.TopRow;
import apidemo.util.HtmlButton;
import apidemo.util.NewTabbedPanel;
import apidemo.util.TCombo;
import apidemo.util.UpperField;
import apidemo.util.VerticalPanel;
import apidemo.util.NewTabbedPanel.NewTabPanel;
import apidemo.util.VerticalPanel.StackPanel;

import com.ib.client.ScannerSubscription;
import com.ib.controller.Bar;
import com.ib.controller.Instrument;
import com.ib.controller.NewContract;
import com.ib.controller.NewContractDetails;
import com.ib.controller.NewOrder;
import com.ib.controller.NewOrderState;
import com.ib.controller.OrderStatus;
import com.ib.controller.OrderType;
import com.ib.controller.ScanCode;
import com.ib.controller.ApiController.IDeepMktDataHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.ApiController.IOrderHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.ApiController.IRealTimeBarHandler;
import com.ib.controller.ApiController.IScannerHandler;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.BarSize;
import com.ib.controller.Types.DeepSide;
import com.ib.controller.Types.DeepType;
import com.ib.controller.Types.DurationUnit;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.TimeInForce;
import com.ib.controller.Types.TradingStatus;
import com.ib.controller.Types.WhatToShow;

class PositionOrderAdapter implements IPositionHandler, ILiveOrderHandler {

	@Override
	public void openOrder(NewContract contract, NewOrder order, NewOrderState orderState) {
		// TODO Auto-generated method stub
		//ApiDemo.INSTANCE.m_mktDataPanel.addContract( contract, 0, 0);
	}

	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void orderStatus(int orderId, OrderStatus status, int filled, int remaining, double avgFillPrice,
			long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handle(int orderId, int errorCode, String errorMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void position(String account, NewContract contract, int position, double avgCost) {
		// TODO Auto-generated method stub
		if ( contract.secType() == SecType.FOP ) {
			contract.exchange("GLOBEX");
			contract.primaryExch("");
			ApiDemo.INSTANCE.getDemoLogger().info("position update:" + contract.description());
			ApiDemo.INSTANCE.m_mktDataPanel.addContract( contract, position, avgCost);
		} else if ( contract.secType() == SecType.OPT ) {
			contract.exchange("SMART");
		}
	}

	@Override
	public void positionEnd() {
		// TODO Auto-generated method stub
	}
}

class OrderTimerActionListener implements ActionListener {
	
	public void actionPerformed(ActionEvent e) {
	    
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		ApiDemo.INSTANCE.getDemoLogger().fine("timer running: "+ dateFormat.format(Calendar.getInstance().getTime()));

		ArrayList<TopRow> contractlist  =   ApiDemo.INSTANCE.m_mktDataPanel.getResultPanel().m_model.getRowsList();

		Iterator<TopRow> itr = contractlist.iterator();
		while (itr.hasNext()) {
			TopRow row = itr.next();
			if (row.getPosition()> 0) {
				ApiDemo.INSTANCE.getDemoLogger().info("Position size is larger than 0");
				continue;
			}

			if ( Calendar.getInstance().compareTo(row.getEnd()) > 0 ) {
				row.closingPrint();
				row.clearLastprint();
				continue;
			}

			if ( Calendar.getInstance().before(row.getStart())) {
				ApiDemo.INSTANCE.getDemoLogger().info("trading session not started, before: "+ dateFormat.format(row.getStart().getTime()));
				continue;
			}

			if ( Calendar.getInstance().after(row.getEnd())) {
				ApiDemo.INSTANCE.getDemoLogger().info("trading session has finished, after: "+ dateFormat.format(row.getEnd().getTime()));
				continue;
			}
			
			if ( row.getAskPrice() <= 0 || row.getBidPrice() <= 0 ) {
				ApiDemo.INSTANCE.getDemoLogger().info("bid or ask is not normal - bid: " + row.getContract().description() + row.getBidPrice() + " ask: " + row.getAskPrice());
				continue;
			}
			
			if ( (row.getAskPrice() - row.getBidPrice())/row.getAskPrice() >= 0.2) {
				ApiDemo.INSTANCE.getDemoLogger().info("too big difference bid/ask:" + row.getContract().description() + " bid: " + row.getBidPrice() + " ask: " + row.getAskPrice());
				continue;
			}

			if ( row.get5sAvg().close() <= 0 ) {
				ApiDemo.INSTANCE.getDemoLogger().info("5sec average is below zero" +  row.get5sAvg().close());
				continue;
			}

			if ( (Math.abs(row.get5sAvg().close() -  (row.getBidPrice() + row.getAskPrice())/2) > 5)) {
				ApiDemo.INSTANCE.getDemoLogger().info("5sec average is too far from midprice");
				continue;
			}

			if (row.getStatus() == TradingStatus.Init) {
				if ( row.getMax() <= 0 || row.getBidPrice() <= 0 || row.getPosition() > 0) {
					;
				} else if ( row.getMax() <= row.getBidPrice()) { //sell
					if ( row.getBoxTradingCounter() >= row.getBoxTradinglimit()) {
						ApiDemo.INSTANCE.getDemoLogger().info("getBoxTradingCounter is larger than limit");
						continue;
					}
					row.setBoxTradingCounter( (row.getBoxTradingCounter()+1));
					row.setStatus(TradingStatus.Selling);
					row.setPrePosition( row.getPosition());
					ApiDemo.INSTANCE.getDemoLogger().info("status changed: Init -> Selling ");
					ApiDemo.INSTANCE.getDemoLogger().info("sell "+row.getContract().description() + " " + Math.abs(row.getUnit()));					
					NewOrder order = new NewOrder();
					order.orderType( OrderType.LMT);
					order.lmtPrice( row.getMax());
					order.tif( TimeInForce.DAY);
					//order.account("DU172556");
					
					order.action(Action.SELL);
					order.outsideRth(true);
					order.totalQuantity( (int)Math.abs(row.getUnit()));
					
					row.getContract().exchange("GLOBEX");
					
					ApiDemo.INSTANCE.controller().placeOrModifyOrder( (NewContract)row.getContract(), order, new IOrderHandler() {
						@Override public void orderState(NewOrderState orderState) {
							ApiDemo.INSTANCE.getDemoLogger().info("order placed");						
						}
						@Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
							if ( status == OrderStatus.Cancelled) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order cancelled");						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else if ( status == OrderStatus.Submitted) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order submitted");						
							} else if ( status == OrderStatus.Filled && remaining == 0) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order filled @avgPrice: " + avgFillPrice);						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else  {
								ApiDemo.INSTANCE.getDemoLogger().info("Order hold: " + whyHeld);	
							}
						}
						@Override public void handle(int errorCode, final String errorMsg) {
							ApiDemo.INSTANCE.getDemoLogger().info("Order code and message: " + errorCode +  " " + errorMsg);						
							//ApiDemo.INSTANCE.controller().removeOrderHandler( this);
						}
					});

				}
			
			} else if (row.getStatus() == TradingStatus.Selling) {
				if (row.getPosition() == row.getPrePosition() && ( row.getLmt() + row.getOffset()- row.get5sAvg().close() < row.get5sAvg().close() - row.getMax())) {
					row.setStatus(TradingStatus.S_M);
					row.setLmt( row.getLmt() + row.getOffset());
					ApiDemo.INSTANCE.getDemoLogger().info("status change: Selling->Sold");
					ApiDemo.INSTANCE.getDemoLogger().info("stop price changed to: " + row.getLmt());
				} else if ( Math.abs(row.getPosition()) == Math.abs(row.getPrePosition()) + row.getUnit()
				&& ( row.getLmt() + row.getOffset() - row.get5sAvg().close() > row.get5sAvg().close() - row.getMax())) {
					row.setStatus(TradingStatus.S_M);
					ApiDemo.INSTANCE.getDemoLogger().info("status change: Selling->Sold");
				}
			} else if (row.getStatus() == TradingStatus.S_M) {
				if ( row.getMin() <= 0 ||  row.getAskPrice() <= 0 || row.getPosition() >= 0) {
					;
				} else if ( row.getMin() >= row.getAskPrice()) { //buy
					if ( row.getBoxTradingCounter() >= row.getBoxTradinglimit()) {
						ApiDemo.INSTANCE.getDemoLogger().info("getBoxTradingCounter is larger than limit");
						continue;
					}
					row.setBoxTradingCounter( (row.getBoxTradingCounter()+1));
					row.setStatus(TradingStatus.Buying);
					row.setPrePosition( row.getPosition());
					ApiDemo.INSTANCE.getDemoLogger().info("status changed: from S_M -> buying ");					
					ApiDemo.INSTANCE.getDemoLogger().info("buy "+row.getContract().description() + " " + Math.abs(row.getUnit()));
					NewOrder order = new NewOrder();
					order.orderType( OrderType.LMT);
					order.lmtPrice( row.getMin());
					order.tif( TimeInForce.DAY);
					//order.account("DU172556");

					order.action(Action.BUY);
					order.outsideRth(true);
					order.totalQuantity( (int)Math.abs(row.getUnit()));
					
					row.getContract().exchange("GLOBEX");

					ApiDemo.INSTANCE.controller().placeOrModifyOrder( (NewContract)row.getContract(), order, new IOrderHandler() {
						@Override public void orderState(NewOrderState orderState) {
							ApiDemo.INSTANCE.getDemoLogger().info("order placed");						
						}
						@Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
							if ( status == OrderStatus.Cancelled) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order cancelled");						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else if ( status == OrderStatus.Submitted) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order submitted");						
							} else if ( status == OrderStatus.Filled && remaining == 0) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order filled @avgPrice: " + avgFillPrice);						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else  {
								ApiDemo.INSTANCE.getDemoLogger().info("Order hold: " + whyHeld);	
							}
						}
						@Override public void handle(int errorCode, final String errorMsg) {
							ApiDemo.INSTANCE.getDemoLogger().info("Order code and message: " + errorCode +  " " + errorMsg);						
							//ApiDemo.INSTANCE.controller().removeOrderHandler( this);
						}
					});
				} else if ( row.getLmt() < row.get5sAvg().close()) {
					if ( row.getlmtTradingCounter() >= row.getStopTradinglimit()) {
						ApiDemo.INSTANCE.getDemoLogger().info("lmtTradingCounter is larger than limit");
						continue;
					}
					row.setlmtTradingCounter( (row.getlmtTradingCounter()+1));
					row.setStatus(TradingStatus.Buying);
					ApiDemo.INSTANCE.getDemoLogger().info("status change: S_M->Buying");

					row.setPrePosition( row.getPosition());
					
					ApiDemo.INSTANCE.getDemoLogger().info("buy "+ row.getContract().description() + " " + Math.abs(row.getPosition()) + " mid: " + row.get5sAvg().close());
					NewOrder order = new NewOrder();
					order.orderType( OrderType.MKT);
					//order.lmtPrice( 150);
					order.tif( TimeInForce.DAY);
					//order.account("DU172556");

					order.action(Action.BUY);
					order.outsideRth(true);
					order.totalQuantity( Math.abs(row.getPosition()));
					row.getContract().exchange("GLOBEX");

					ApiDemo.INSTANCE.controller().placeOrModifyOrder( (NewContract)row.getContract(), order, new IOrderHandler() {
						@Override public void orderState(NewOrderState orderState) {
							ApiDemo.INSTANCE.getDemoLogger().info("order placed");						
						}
						@Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
							if ( status == OrderStatus.Cancelled) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order cancelled");						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else if ( status == OrderStatus.Submitted) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order submitted");						
							} else if ( status == OrderStatus.Filled && remaining == 0) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order filled @avgPrice: " + avgFillPrice);						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else  {
								ApiDemo.INSTANCE.getDemoLogger().info("Order hold: " + whyHeld);	
							}
						}
						@Override public void handle(int errorCode, final String errorMsg) {
							ApiDemo.INSTANCE.getDemoLogger().info("Order code and message: " + errorCode +  " " + errorMsg);						
							//ApiDemo.INSTANCE.controller().removeOrderHandler( this);
						}
					});
				}
			} else if (row.getStatus() == TradingStatus.Buying) {
				if (  (row.getPosition() == 0 ) && (row.get5sAvg().close() >= row.getLmt())) {
					if ( row.getLmt() + row.getOffset() < row.get5sAvg().close()) {
						row.setStatus(TradingStatus.B_L_O);
						ApiDemo.INSTANCE.getDemoLogger().info("status change: Buying->B_L_O");
					}   else  {
						row.setStatus(TradingStatus.B_L);
						ApiDemo.INSTANCE.getDemoLogger().info("status change: Buying->B_L");
					} 					
				} else	if ( (Math.abs(row.getPrePosition()) == Math.abs(row.getPosition()) + row.getUnit()) && (row.getMin()  >  row.getAskPrice())) {
						row.setStatus(TradingStatus.B_M);
						ApiDemo.INSTANCE.getDemoLogger().info("status change: Buying->B_M");
				}			
			} else if (row.getStatus() == TradingStatus.B_M) {
				if ( row.getMax() <= 0 || row.getBidPrice() <= 0 || row.getPosition() > 0) {
					;
				} else if ( row.getMax() <= row.getBidPrice()) { //sell
					if ( row.getBoxTradingCounter() >= row.getBoxTradinglimit()) {
						ApiDemo.INSTANCE.getDemoLogger().info("BoxTradingCounter is larger than limit");
						continue;
					}
					row.setBoxTradingCounter( (row.getBoxTradingCounter()+1));
					row.setStatus(TradingStatus.Selling);
					row.setPrePosition( row.getPosition());
					ApiDemo.INSTANCE.getDemoLogger().info("status changed: from B_M -> Selling ");
					ApiDemo.INSTANCE.getDemoLogger().info("sell "+row.getContract().description() + " " + Math.abs(row.getUnit()));					
					NewOrder order = new NewOrder();
					order.orderType( OrderType.LMT);
					order.lmtPrice( row.getMax());
					order.tif( TimeInForce.DAY);
					//order.account("DU172556");
					
					order.action(Action.SELL);
					order.outsideRth(true);
					order.totalQuantity( (int)Math.abs(row.getUnit()));
					
					row.getContract().exchange("GLOBEX");
					
					ApiDemo.INSTANCE.controller().placeOrModifyOrder( (NewContract)row.getContract(), order, new IOrderHandler() {
						@Override public void orderState(NewOrderState orderState) {
							ApiDemo.INSTANCE.getDemoLogger().info("order placed");						
						}
						@Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
							if ( status == OrderStatus.Cancelled) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order cancelled");						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else if ( status == OrderStatus.Submitted) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order submitted");						
							} else if ( status == OrderStatus.Filled && remaining == 0) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order filled @avgPrice: " + avgFillPrice);						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else  {
								ApiDemo.INSTANCE.getDemoLogger().info("Order hold: " + whyHeld);	
							}
						}
						@Override public void handle(int errorCode, final String errorMsg) {
							ApiDemo.INSTANCE.getDemoLogger().info("Order code and message: " + errorCode +  " " + errorMsg);						
							//ApiDemo.INSTANCE.controller().removeOrderHandler( this);
						}
					});
				}
			
			} else if (row.getStatus() == TradingStatus.B_L_O) {
				if (row.getLmt() + row.getOffset() > row.get5sAvg().close()) { //sell
					if ( row.getlmtTradingCounter() >= row.getStopTradinglimit()) {
						ApiDemo.INSTANCE.getDemoLogger().info("lmtTradingCounter is larger than limit");
						continue;
					}
					row.setlmtTradingCounter( (row.getlmtTradingCounter()+1));
					row.setStatus(TradingStatus.Selling);
					ApiDemo.INSTANCE.getDemoLogger().info("status change: B_L_O->Selling");
					
					ApiDemo.INSTANCE.getDemoLogger().info("sell "+row.getContract().description() + " " + Math.abs(row.getPrePosition())   + " mid: " + row.get5sAvg().close());
					NewOrder order = new NewOrder();
					order.orderType( OrderType.MKT);
					//order.lmtPrice( 150);
					order.tif( TimeInForce.DAY);
					//order.account("DU172556");
				
					order.action(Action.SELL);
					order.outsideRth(true);
					order.totalQuantity( (int)Math.abs(row.getPrePosition()));
					row.setPrePosition(row.getPrePosition() + row.getUnit());
					row.getContract().exchange("GLOBEX");

					ApiDemo.INSTANCE.controller().placeOrModifyOrder( (NewContract)row.getContract(), order, new IOrderHandler() {
						@Override public void orderState(NewOrderState orderState) {
							ApiDemo.INSTANCE.getDemoLogger().info("order placed");						
						}
						@Override public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
							if ( status == OrderStatus.Cancelled) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order cancelled");						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else if ( status == OrderStatus.Submitted) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order submitted");						
							} else if ( status == OrderStatus.Filled && remaining == 0) {
								ApiDemo.INSTANCE.getDemoLogger().info("Order filled @avgPrice: " + avgFillPrice);						
								ApiDemo.INSTANCE.controller().removeOrderHandler( this);
							} else  {
								ApiDemo.INSTANCE.getDemoLogger().info("Order hold: " + whyHeld);	
							}
						}
						@Override public void handle(int errorCode, final String errorMsg) {
							ApiDemo.INSTANCE.getDemoLogger().info("Order code and message: " + errorCode +  " " + errorMsg);						
							//ApiDemo.INSTANCE.controller().removeOrderHandler( this);
						}
					});
				}				
			}	else if (row.getStatus() == TradingStatus.B_L) {
				if ( row.getLmt() + row.getOffset() < row.get5sAvg().close()  ) {
					row.setStatus(TradingStatus.B_L_O);
					ApiDemo.INSTANCE.getDemoLogger().info("status change: B_L->B_L_O");
				}			
			}

		}
	    
	}
}

public class MarketDataPanel extends JPanel {
	private final NewContract m_contract = new NewContract();
	private final NewTabbedPanel m_requestPanel = new NewTabbedPanel();
    // jicheng change to static public
	private final NewTabbedPanel m_resultsPanel = new NewTabbedPanel();
	//private final TopResultsPanel m_topResultPane;
	private final TopResultsPanel m_topResultPanel = new TopResultsPanel();	
	private final PositionOrderAdapter position_order_callback = new PositionOrderAdapter();
	
	MarketDataPanel() {
		m_requestPanel.addTab( "Top Market Data", new TopRequestPanel() );
		m_requestPanel.addTab( "Deep Book", new DeepRequestPanel() );
		m_requestPanel.addTab( "Historical Data", new HistRequestPanel() );
		m_requestPanel.addTab( "Real-time Bars", new RealtimeRequestPanel() );
		m_requestPanel.addTab( "Market Scanner", new ScannerRequestPanel() );

		m_resultsPanel.addTab( "Top Data", m_topResultPanel, true, true);
		
		setLayout( new BorderLayout() );
		add( m_requestPanel, BorderLayout.NORTH);
		add( m_resultsPanel);
		
		// jicheng  add timer
	    //Timer timer = new Timer(5000, new MyTimerActionListener());
		//timer.start();
	    //ApiDemo.INSTANCE.controller().reqPositions(ApiDemo.INSTANCE.m_mktDataPanel.getCallback());

	    Timer ordertimer = new Timer(200, new OrderTimerActionListener()); 
	    ordertimer.start();
	}

	public TopResultsPanel getResultPanel() {
		return m_topResultPanel;
	}
	
	public PositionOrderAdapter getCallback() {
		return position_order_callback;
	}
	// jicheng
	void addContract( NewContract contract, int position, double avgCost) {
        m_topResultPanel.m_model.addRow( contract, position, avgCost);
	}
	
	private class TopRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		
		TopRequestPanel() {
			HtmlButton reqTop = new HtmlButton( "Request Top Market Data") {
				@Override protected void actionPerformed() {
					onTop();
				}
			};
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( reqTop);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( butPanel);
		}

		protected void onTop() {
			m_contractPanel.onOK();
		//if (m_topResultPanel == null) {
		//		m_topResultPanel = new TopResultsPanel();
		//		m_resultsPanel.addTab( "Top Data", m_topResultPanel, true, true);
		//	}
			m_topResultPanel.m_model.addRow( m_contract.clone(), 0, 0);
			
		}
	}
	
	class TopResultsPanel extends NewTabPanel {
		final TopModel m_model = new TopModel();
		final JTable m_tab = new TopTable( m_model);
		final TCombo<MktDataType> m_typeCombo = new TCombo<MktDataType>( MktDataType.values() );

		TopResultsPanel() {
			m_typeCombo.removeItemAt( 0);

			m_tab.setRowSelectionAllowed(true); //jicheng
	        //KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
	        //m_tab.getActionMap().put("delete", deleteAction());
	        //m_tab.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, "delete");

			JScrollPane scroll = new JScrollPane( m_tab);

			HtmlButton reqType = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onReqType();
				}
			};

			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( "Market data type", m_typeCombo, reqType);
			
			setLayout( new BorderLayout() );
			add( scroll);
			add( butPanel, BorderLayout.SOUTH);
		}


		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			m_model.desubscribe();
			//m_topResultPanel = null;
		}

		void onReqType() {
			ApiDemo.INSTANCE.controller().reqMktDataType( m_typeCombo.getSelectedItem() );
		}
		
		class TopTable extends JTable {
			public TopTable(TopModel model) { super( model); }

			@Override public TableCellRenderer getCellRenderer(int rowIn, int column) {
				TableCellRenderer rend = super.getCellRenderer(rowIn, column);
				m_model.color( rend, rowIn, getForeground() );
				return rend;
			}
		}
	}		
	
	private class DeepRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		
		DeepRequestPanel() {
			HtmlButton reqDeep = new HtmlButton( "Request Deep Market Data") {
				@Override protected void actionPerformed() {
					onDeep();
				}
			};
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( reqDeep);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( butPanel);
		}

		protected void onDeep() {
			m_contractPanel.onOK();
			DeepResultsPanel resultPanel = new DeepResultsPanel();
			m_resultsPanel.addTab( "Deep " + m_contract.symbol(), resultPanel, true, true);
			ApiDemo.INSTANCE.controller().reqDeepMktData(m_contract, 6, resultPanel);
		}
	}

	private static class DeepResultsPanel extends NewTabPanel implements IDeepMktDataHandler {
		final DeepModel m_buy = new DeepModel();
		final DeepModel m_sell = new DeepModel();

		DeepResultsPanel() {
			HtmlButton desub = new HtmlButton( "Desubscribe") {
				public void actionPerformed() {
					onDesub();
				}
			};
			
			JTable buyTab = new JTable( m_buy);
			JTable sellTab = new JTable( m_sell);
			
			JScrollPane buyScroll = new JScrollPane( buyTab);
			JScrollPane sellScroll = new JScrollPane( sellTab);
			
			JPanel mid = new JPanel( new GridLayout( 1, 2) );
			mid.add( buyScroll);
			mid.add( sellScroll);
			
			setLayout( new BorderLayout() );
			add( mid);
			add( desub, BorderLayout.SOUTH);
		}
		
		protected void onDesub() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}

		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelDeepMktData( this);
		}
		
		@Override public void updateMktDepth(int pos, String mm, DeepType operation, DeepSide side, double price, int size) {
			if (side == DeepSide.BUY) {
				m_buy.updateMktDepth(pos, mm, operation, price, size);
			}
			else {
				m_sell.updateMktDepth(pos, mm, operation, price, size);
			}
		}

		class DeepModel extends AbstractTableModel {
			final ArrayList<DeepRow> m_rows = new ArrayList<DeepRow>();

			@Override public int getRowCount() {
				return m_rows.size();
			}

			public void updateMktDepth(int pos, String mm, DeepType operation, double price, int size) {
				switch( operation) {
					case INSERT:
						m_rows.add( pos, new DeepRow( mm, price, size) );
						fireTableRowsInserted(pos, pos);
						break;
					case UPDATE:
						m_rows.get( pos).update( mm, price, size);
						fireTableRowsUpdated(pos, pos);
						break;
					case DELETE:
						if (pos < m_rows.size() ) {
							m_rows.remove( pos);
						}
						else {
							// this happens but seems to be harmless
							// System.out.println( "can't remove " + pos);
						}
						fireTableRowsDeleted(pos, pos);
						break;
				}
			}

			@Override public int getColumnCount() {
				return 3;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Mkt Maker";
					case 1: return "Price";
					case 2: return "Size";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				DeepRow row = m_rows.get( rowIn);
				
				switch( col) {
					case 0: return row.m_mm;
					case 1: return row.m_price;
					case 2: return row.m_size;
					default: return null;
				}
			}
		}
		
		static class DeepRow {
			String m_mm;
			double m_price;
			int m_size;

			public DeepRow(String mm, double price, int size) {
				update( mm, price, size);
			}
			
			void update( String mm, double price, int size) {
				m_mm = mm;
				m_price = price;
				m_size = size;
			}
		}
	}

	private class HistRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final UpperField m_end = new UpperField();
		final UpperField m_duration = new UpperField();
		final TCombo<DurationUnit> m_durationUnit = new TCombo<DurationUnit>( DurationUnit.values() );
		final TCombo<BarSize> m_barSize = new TCombo<BarSize>( BarSize.values() );
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		HistRequestPanel() { 		
			m_end.setText( "20120101 12:00:00");
			m_duration.setText( "1");
			m_durationUnit.setSelectedItem( DurationUnit.WEEK);
			m_barSize.setSelectedItem( BarSize._1_hour);
			
			HtmlButton button = new HtmlButton( "Request historical data") {
				@Override protected void actionPerformed() {
					onHistorical();
				}
			};
			
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "End", m_end);
			paramPanel.add( "Duration", m_duration);
			paramPanel.add( "Duration unit", m_durationUnit);
			paramPanel.add( "Bar size", m_barSize);
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onHistorical() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( true);
			ApiDemo.INSTANCE.controller().reqHistoricalData(m_contract, m_end.getText(), m_duration.getInt(), m_durationUnit.getSelectedItem(), m_barSize.getSelectedItem(), m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_resultsPanel.addTab( "Historical " + m_contract.symbol(), panel, true, true);
		}
	}

	private class RealtimeRequestPanel extends JPanel {
		final ContractPanel m_contractPanel = new ContractPanel(m_contract);
		final TCombo<WhatToShow> m_whatToShow = new TCombo<WhatToShow>( WhatToShow.values() );
		final JCheckBox m_rthOnly = new JCheckBox();
		
		RealtimeRequestPanel() { 		
			HtmlButton button = new HtmlButton( "Request real-time bars") {
				@Override protected void actionPerformed() {
					onRealTime();
				}
			};
	
	    	VerticalPanel paramPanel = new VerticalPanel();
			paramPanel.add( "What to show", m_whatToShow);
			paramPanel.add( "RTH only", m_rthOnly);
			
			VerticalPanel butPanel = new VerticalPanel();
			butPanel.add( button);
			
			JPanel rightPanel = new StackPanel();
			rightPanel.add( paramPanel);
			rightPanel.add( Box.createVerticalStrut( 20));
			rightPanel.add( butPanel);
			
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20) );
			add( rightPanel);
		}
	
		protected void onRealTime() {
			m_contractPanel.onOK();
			BarResultsPanel panel = new BarResultsPanel( false);
			ApiDemo.INSTANCE.controller().reqRealTimeBars(m_contract, m_whatToShow.getSelectedItem(), m_rthOnly.isSelected(), panel);
			m_resultsPanel.addTab( "Real-time " + m_contract.symbol(), panel, true, true);
		}
	}
	
	static class BarResultsPanel extends NewTabPanel implements IHistoricalDataHandler, IRealTimeBarHandler {
		final BarModel m_model = new BarModel();
		final ArrayList<Bar> m_rows = new ArrayList<Bar>();
		final boolean m_historical;
		final Chart m_chart = new Chart( m_rows);
		
		BarResultsPanel( boolean historical) {
			m_historical = historical;
			
			JTable tab = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( tab) {
				public Dimension getPreferredSize() {
					Dimension d = super.getPreferredSize();
					d.width = 500;
					return d;
				}
			};

			JScrollPane chartScroll = new JScrollPane( m_chart);

			setLayout( new BorderLayout() );
			add( scroll, BorderLayout.WEST);
			add( chartScroll, BorderLayout.CENTER);
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			if (m_historical) {
				ApiDemo.INSTANCE.controller().cancelHistoricalData( this);
			}
			else {
				ApiDemo.INSTANCE.controller().cancelRealtimeBars( this);
			}
		}

		@Override public void historicalData(Bar bar, boolean hasGaps) {
			m_rows.add( bar);
		}
		
		@Override public void historicalDataEnd() {
			fire();
		}

		@Override public void realtimeBar(Bar bar) {
			m_rows.add( bar); 
			fire();
		}
		
		private void fire() {
			SwingUtilities.invokeLater( new Runnable() {
				@Override public void run() {
					m_model.fireTableRowsInserted( m_rows.size() - 1, m_rows.size() - 1);
					m_chart.repaint();
				}
			});
		}

		class BarModel extends AbstractTableModel {
			@Override public int getRowCount() {
				return m_rows.size();
			}

			@Override public int getColumnCount() {
				return 7;
			}
			
			@Override public String getColumnName(int col) {
				switch( col) {
					case 0: return "Date/time";
					case 1: return "Open";
					case 2: return "High";
					case 3: return "Low";
					case 4: return "Close";
					case 5: return "Volume";
					case 6: return "WAP";
					default: return null;
				}
			}

			@Override public Object getValueAt(int rowIn, int col) {
				Bar row = m_rows.get( rowIn);
				switch( col) {
					case 0: return row.formattedTime();
					case 1: return row.open();
					case 2: return row.high();
					case 3: return row.low();
					case 4: return row.close();
					case 5: return row.volume();
					case 6: return row.wap();
					default: return null;
				}
			}
		}		
	}
	
	private class ScannerRequestPanel extends JPanel {
		final UpperField m_numRows = new UpperField( "15");
		final TCombo<ScanCode> m_scanCode = new TCombo<ScanCode>( ScanCode.values() );
		final TCombo<Instrument> m_instrument = new TCombo<Instrument>( Instrument.values() );
		final UpperField m_location = new UpperField( "STK.US.MAJOR", 9);
		final TCombo<String> m_stockType = new TCombo<String>( "ALL", "STOCK", "ETF");
		
		ScannerRequestPanel() {
			HtmlButton go = new HtmlButton( "Go") {
				@Override protected void actionPerformed() {
					onGo();
				}
			};
			
			VerticalPanel paramsPanel = new VerticalPanel();
			paramsPanel.add( "Scan code", m_scanCode);
			paramsPanel.add( "Instrument", m_instrument);
			paramsPanel.add( "Location", m_location, Box.createHorizontalStrut(10), go);
			paramsPanel.add( "Stock type", m_stockType);
			paramsPanel.add( "Num rows", m_numRows);
			
			setLayout( new BorderLayout() );
			add( paramsPanel, BorderLayout.NORTH);
		}

		protected void onGo() {
			ScannerSubscription sub = new ScannerSubscription();
			sub.numberOfRows( m_numRows.getInt() );
			sub.scanCode( m_scanCode.getSelectedItem().toString() );
			sub.instrument( m_instrument.getSelectedItem().toString() );
			sub.locationCode( m_location.getText() );
			sub.stockTypeFilter( m_stockType.getSelectedItem().toString() );
			
			ScannerResultsPanel resultsPanel = new ScannerResultsPanel();
			m_resultsPanel.addTab( sub.scanCode(), resultsPanel, true, true);

			ApiDemo.INSTANCE.controller().reqScannerSubscription( sub, resultsPanel);
		}
	}

	static class ScannerResultsPanel extends NewTabPanel implements IScannerHandler {
		final HashSet<Integer> m_conids = new HashSet<Integer>();
		final TopModel m_model = new TopModel();

		ScannerResultsPanel() {
			JTable table = new JTable( m_model);
			JScrollPane scroll = new JScrollPane( table);
			setLayout( new BorderLayout() );
			add( scroll);
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
			ApiDemo.INSTANCE.controller().cancelScannerSubscription( this);
			m_model.desubscribe();
		}

		@Override public void scannerParameters(String xml) {
			try {
				File file = File.createTempFile( "pre", ".xml");
				FileWriter writer = new FileWriter( file);
				writer.write( xml);
				writer.close();

				Desktop.getDesktop().open( file);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override public void scannerData(int rank, final NewContractDetails contractDetails, String legsStr) {
			if (!m_conids.contains( contractDetails.conid() ) ) {
				m_conids.add( contractDetails.conid() );
				SwingUtilities.invokeLater( new Runnable() {
					@Override public void run() {
						m_model.addRow( contractDetails.contract(), 0, 0 );
					}
				});
			}
		}

		@Override public void scannerDataEnd() {
			// we could sort here
		}
	}
}
