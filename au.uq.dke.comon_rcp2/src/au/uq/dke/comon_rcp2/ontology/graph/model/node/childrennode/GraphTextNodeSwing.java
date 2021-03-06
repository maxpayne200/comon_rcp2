package au.uq.dke.comon_rcp2.ontology.graph.model.node.childrennode;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import ca.uvic.cs.chisel.cajun.graph.node.IGraphNode;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;

public class GraphTextNodeSwing extends PText {
	private static final long serialVersionUID = -871571524212274580L;

	private boolean ignoreInvalidatePaint = false;

	private IGraphNode graphNode;

	
	public GraphTextNodeSwing(IGraphNode graphNode){
		this.graphNode = graphNode;
	}

	@Override
	public Font getFont() {
		Font font = null;
		if (font == null) {
			font = DEFAULT_FONT;
		}
		return font;
	}

	@Override
	public Paint getTextPaint() {
		Paint paint = null;
		if (paint == null) {
			paint = Color.black;
		}
		return paint;
	}

	@Override
	protected void paint(PPaintContext paintContext) {
		// update the text paint - the super paint method doesn't call our
		// getTextPaint() method
		Paint p = getTextPaint();
		if (!p.equals(super.getTextPaint())) {
			ignoreInvalidatePaint = true;
			setTextPaint(getTextPaint());
			ignoreInvalidatePaint = false;
		}
		// the font is never set in the super paint class?
		paintContext.getGraphics().setFont(getFont());
		super.paint(paintContext);
	}

	@Override
	public void invalidatePaint() {
		if (!ignoreInvalidatePaint) {
			super.invalidatePaint();
		}
	}

}
