package ca.uvic.cs.chisel.cajun.actions;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Icon;

import org.eclipse.zest.layouts.InvalidLayoutConfiguration;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.DirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.progress.ProgressListener;

import au.uq.dke.comon_rcp2.application.views.graph.OntologyGraph;
import ca.uvic.cs.chisel.cajun.graph.IGraph;
import ca.uvic.cs.chisel.cajun.graph.arc.DefaultGraphArc;
import ca.uvic.cs.chisel.cajun.graph.arc.IGraphArc;
import ca.uvic.cs.chisel.cajun.graph.node.DefaultGraphNode;
import ca.uvic.cs.chisel.cajun.graph.node.IGraphNode;
import ca.uvic.cs.chisel.cajun.graph.util.ActivityManager;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.util.PUtil;
import edu.umd.cs.piccolox.swt.PSWTCanvas;

public class LayoutManager extends CajunAction {
	private static final long serialVersionUID = -7385859217531335673L;
	
	private static final int MAX_NODES_TO_ANIMATE = 200;
	private static final double DELTA = 0.01;

	private static LayoutAlgorithm CURRENT_LAYOUT_ALGORITHM = new DirectedGraphLayoutAlgorithm();
	private boolean animate;
	private int maxNodesToAnimate = MAX_NODES_TO_ANIMATE;
	private boolean resizeNodes;
	
	private ActivityManager activityManager;

	/** list of relationship types that the layout should be applied to */
	private List<Object> layoutRelTypes;
	
	public LayoutManager(String name, Icon icon, LayoutAlgorithm layout, IGraph graph) {
		this(name, icon, layout, graph, true);
	}

	public LayoutManager(String name, Icon icon, LayoutAlgorithm layoutAlgorithm, IGraph graph, boolean animate) {
		super(name, icon);
		CURRENT_LAYOUT_ALGORITHM = layoutAlgorithm;
		this.animate = animate;
		this.resizeNodes = false;
		this.layoutRelTypes = new ArrayList<Object>();
		
		this.activityManager = new ActivityManager(graph.getCanvas(), graph.getCanvas().getRoot().getActivityScheduler());
	}
	
	public LayoutAlgorithm getLayout() {
		return CURRENT_LAYOUT_ALGORITHM;
	}

	public void setLayout(LayoutAlgorithm layout) {
		this.CURRENT_LAYOUT_ALGORITHM = layout;
	}

	public void setLayoutRelTypes(List<Object> layoutRelTypes) {
		this.layoutRelTypes = layoutRelTypes;
	}
	
	public void addProgressListener(ProgressListener listener) {
		activityManager.addProgressListener(listener);
	}

	public void doAction() {
		// save this action as the last executed action
		OntologyGraph.getInstance().setLastLayout(this);
		runLayout();
	}
	
	public void runLayout() {
		// run the layout only on the visible nodes?  Or all nodes?
		Collection<IGraphNode> nodes = OntologyGraph.getInstance().getModel().getVisibleNodes();
		Collection<IGraphArc> arcs = OntologyGraph.getInstance().getModel().getVisibleArcs();
		DefaultGraphNode[] entities = nodes.toArray(new DefaultGraphNode[nodes.size()]);
		
		Collection<IGraphArc> filteredArcs;
		if (layoutRelTypes.isEmpty()) {
			// no arcs in the list - so assume all arcs should be used in the layout
			filteredArcs = arcs;
		} else {
			// remove arcs that have been filtered
			filteredArcs = new ArrayList<IGraphArc>();
			for (IGraphArc arc : arcs) {
				if (layoutRelTypes.contains(arc.getType())) {
					filteredArcs.add(arc);
				}
			}
		}
		
		DefaultGraphArc[] rels = filteredArcs.toArray(new DefaultGraphArc[filteredArcs.size()]);

		PSWTCanvas canvas = OntologyGraph.getInstance().getCanvas();

		double x = 0, y = 0;
//		double w = Math.max(0, canvas.getWidth() - 10);
//		double h = Math.max(0, canvas.getHeight() - 10);
		double w = 1500;
		double h = 1500;

		// to allow extra room for wide nodes
		if (w > 400) {
			w -= 100;
		}
		// extra room for tall nodes (labels wrap)
		if (h > 300) {
			h -= 30;
		}
	
		try {
			// define a local version of the layout in order to avoid threading issues
			CURRENT_LAYOUT_ALGORITHM.applyLayout(entities, rels, x, y, w, h, false, false);

			if (animate && (nodes.size() > maxNodesToAnimate)) {
				animate = false;
			}

			//PActivityScheduler scheduler = canvas.getRoot().getActivityScheduler();
			ArrayList<PActivity> activities = new ArrayList<PActivity>(nodes.size());
			
			for (IGraphNode node : nodes) {
				if(!node.isFixedLocation()) {
					if (animate) {
						AffineTransform transform = createTransform(node);
						PActivity activity = createActivity(node, transform);
						if (activity != null) {
							activities.add(activity);
						}
					} else {
						node.setLocation(node.getXInLayout(), node.getYInLayout());
					}
				}
			}
			
			if (animate) {
				//ActivityManager manager = new ActivityManager(canvas, scheduler, activities);
				activityManager.setActivities(activities);
				// wait until all nodes have finished moving
				// @tag question : why did Chris put this in here?  it blocks the UI thread
				//manager.waitForActivitiesToFinish();
			} else {
				canvas.repaint();
			}
			
			// ensure that the first selected node is visible in the scroll pane
//			Collection<GraphNode> selectedNodes = graph.getSelectedNodes();
//			if (selectedNodes.size() > 0) {
//				GraphNode first = selectedNodes.iterator().next();
//				Rectangle2D bounds = first.getBounds();
//				//graph.getCanvas().scrollRectToVisible(bounds.getBounds());
//			}

		} catch (InvalidLayoutConfiguration e) {
			e.printStackTrace();
		}
	}

	protected AffineTransform createTransform(IGraphNode node) {
		Rectangle2D bounds = node.getBounds();
		double oldW = bounds.getWidth();
		double oldH = bounds.getHeight();
		double newW = node.getWidthInLayout();
		double newH = node.getHeightInLayout();
		double dw = newW - oldW;
		double dh = newH - oldH;

		double dx = (node.getXInLayout() - bounds.getX());
		double dy = (node.getYInLayout() - bounds.getY());

		AffineTransform at = new AffineTransform();
		boolean valid = false;
		if ((Math.abs(dx) > DELTA) || (Math.abs(dy) > DELTA)) {
			at.translate(dx, dy);
			valid = true;
		}
		if (resizeNodes && ((oldW != 0) && (oldH != 0)) && ((Math.abs(dw) > DELTA) || (Math.abs(dh) > DELTA))) {
			double sx = (newW / oldW);
			double sy = (newH / oldH);
			// TODO I don't know if this actually works!
			at.scale(sx, sy);
			valid = true;
		}
		if (!valid) {
			at = null;
		}
		return at;
	}

	/**
	 * See ca.uvic.csr.shrimp.DisplayBean.AbstractDisplayBean#
	 * setTransformsOfNodesWithAnimation(java.util.List, java.util.List)
	 */
	protected PActivity createActivity(final IGraphNode node, AffineTransform transform) {
		Rectangle2D bounds = node.getBounds();
		final double startX = bounds.getX();
		final double startY = bounds.getY();
		PTransformActivity.Target t = new PTransformActivity.Target() {
			public void setTransform(AffineTransform at) {
				//node.setTransform(at);
				node.setLocation(startX + at.getTranslateX(), startY + at.getTranslateY());
			}

			public void getSourceMatrix(double[] aSource) {
				if (node instanceof PNode) {
					((PNode) node).getTransformReference(true).getMatrix(aSource);
				}
			}
		};
		PActivity activity = new PTransformActivity(1500, PUtil.DEFAULT_ACTIVITY_STEP_RATE, t, transform);
		return activity;
	}

	/**
	 * Creates a new instance of the LayoutAlgorithm using reflection.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private LayoutAlgorithm getLayoutAlgorithm() {
		Class<LayoutAlgorithm> c;
		try {
			c = (Class<LayoutAlgorithm>) Class.forName(CURRENT_LAYOUT_ALGORITHM.getClass().getName());
			return c.newInstance();
		} catch (ClassNotFoundException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			//  Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static LayoutAlgorithm getCURRENT_LAYOUT_ALGORITHM() {
		return CURRENT_LAYOUT_ALGORITHM;
	}

	public static void setCURRENT_LAYOUT_ALGORITHM(
			LayoutAlgorithm cURRENT_LAYOUT_ALGORITHM) {
		CURRENT_LAYOUT_ALGORITHM = cURRENT_LAYOUT_ALGORITHM;
	}
}
