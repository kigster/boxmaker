/*
 *	A wrapper for PSGr to draw boxes.  Initialize, then call drawBox!
 *
 *	2004.03.10 (rahulb)
 *		- created, gleaning code out of BoxMaker.java

*/

package com.rahulbotics.boxmaker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
 
/**
 * Handles actually drawing of the notched box to a file.  This class passes everything around
 * in millimeters until it actually draws it at the low level.  It renders a files like this:
 * <pre>
 *               ----------
 *               |  w x d |
 *               ----------
 *               ----------
 *               |  w x h |
 *               |        |
 *               ----------
 *    ---------  ----------  ---------
 *    | d x h |  |  w x d |  | d x h |
 *    ---------  ----------  ---------
 *               ----------
 *               |  w x h |
 *               |        |
 *               ----------
 * </pre>
 *  
 * @author rahulb
 */
public class Renderer {

	// how many millimeters in one inch
	static final float MM_PER_INCH = 25.4f;
	// how many inches in one millimeter
    static final float INCH_PER_MM = 0.0393700787f;
    // the standard display DPI of the pdf (not the same as printing resolution to a pdf)
    static final float DPI = 72.0f;

    // the PDF document created
    private Document doc;
    // the writer underneath the PDF document, which we need to keep a reference to
    private PdfWriter docPdfWriter;
    // the path that we are writing the file to
    private String filePath; 

    /**
     * Public method to render and save a box.
     *  
     * @param filePath			the full absolute path to save the file to
     * @param mmWidth			the width of the box in millimeters
     * @param mmHeight			the height of the box in millimeters
     * @param mmDepth			the depth of the box in millimeters
     * @param mmThickness		the thickness of the material in millimeters
     * @param mmCutWidth		the width of the laser beam
     * @param mmNotchLength		the length of the notch to use to hold the box together
     * @param drawBoundingBox	draw an outer edge with a dimension (for easier DXF import)
     * @param specifiedInInches the user specified the box in inches?
     *
     * @throws FileNotFoundException
     * @throws DocumentException
     */
    public static void render(String filePath,double mmWidth,double mmHeight,
                              double mmDepth,double mmThickness, double mmCutWidth,double mmNotchLength,
                              boolean drawBoundingBox,boolean specifiedInInches) 
        throws FileNotFoundException, DocumentException {
    	Renderer myRenderer = new Renderer(filePath);
    	myRenderer.drawAllSides(mmWidth,mmHeight,mmDepth,mmThickness,mmCutWidth,mmNotchLength,
                                drawBoundingBox,specifiedInInches);
    	myRenderer.closeDoc();
    }
    
	/**
	 * Create a new renderer (doesn't actually do anything)
	 * @param pathToSave	the full absolute path to save the file to
	 */
	private Renderer(String pathToSave){
    	filePath = pathToSave;
    }
    
    /**
     * Create the document to write to (needed before any rendering can happen).
     * @param widthMm	the width of the document in millimeters
     * @param heightMm	the height of the document in millimeters
     * @throws FileNotFoundException
     * @throws DocumentException
     */
    private void openDoc(float widthMm,float heightMm) throws FileNotFoundException, DocumentException{
		float docWidth = widthMm*DPI;
		float docHeight = heightMm*DPI;
		//System.out.println("doc = "+docWidth+" x "+docHeight);
    	doc = new Document(new Rectangle(docWidth,docHeight));
		docPdfWriter = PdfWriter.getInstance(doc,new FileOutputStream(filePath));
		String appNameVersion = BoxMakerConstants.APP_NAME+" "+BoxMakerConstants.VERSION;
		doc.addAuthor(appNameVersion);
		doc.open();
		doc.add(new Paragraph(
                    "Produced by "+BoxMakerConstants.APP_NAME+" "+BoxMakerConstants.VERSION+"\n"+
                    "  on "+new Date()+"\n"+BoxMakerConstants.WEBSITE_URL )
                );
    }

    /**
     * Draw a bounding box around the whole thing.
     * 
     * @param margin	the offset to draw the box (in millimeters)
     * @param widthMM	the width of the box to draw (in millimeters)
     * @param heightMM	the height of the box to draw (in millimeters)
     * @throws DocumentException 
     */
    private void drawBoundingBox(float margin,float widthMM, float heightMM, boolean specifiedInInches) 
        throws DocumentException {
    	drawBoxByMm(margin, margin, widthMM, heightMM);
		if(specifiedInInches) {
            doc.add(new Paragraph("Bounding box (in): "+widthMM+" x "+heightMM));
		} else {
		    doc.add(new Paragraph("Bounding box (mm): "+widthMM * MM_PER_INCH+" x "+heightMM * MM_PER_INCH));
		}
	}

    /**
     * Close up the document (writing it to disk)
     */
    private void closeDoc(){
		doc.close();    	
    }
    
	/**
	 * Math utility function
	 * @param numd	a number
	 * @return		the closest odd number to the one passed in
	 */
	private static int closestOddTo(double numd){
		int num=(int) (numd+0.5);
		if(num % 2 == 0) return num-1;
		return num;
    }
	
    /**
     * Actually draw all the faces of the box
     * @param mmWidth			the width of the box in millimeters
     * @param mmHeight			the height of the box in millimeters
     * @param mmDepth			the depth of the box in millimeters
     * @param mmThickness		the thickness of the material in millimeters
     * @param mmCutWidth		the width of the laser beam
     * @param mmNotchLength		the length of the notch to use to hold the box together
     * @param drawBoundingBox 	draw an outer edge with a dimension (for easier DXF import)
     * @param specifiedInInches the user specified the box in inches?
     * @throws FileNotFoundException
     * @throws DocumentException
     */
    public void drawAllSides(double mmWidth,double mmHeight,double mmDepth,double mmThickness,
                             double mmCutWidth,double mmNotchLength, boolean drawBoundingBox,
                             boolean specifiedInInches) 
        throws FileNotFoundException, DocumentException{
		float width =  (float) mmWidth;
		float height =  (float) mmHeight;
		float depth =  (float) mmDepth;
		float thickness =  (float) mmThickness;
		float notchLength =  (float) mmNotchLength;
		float cutwidth = (float) mmCutWidth;
		
		// enlarge the box to compensate for cut width
		width+=cutwidth;
		height+=cutwidth;
		depth+=cutwidth;
	
		//figure out how many notches for each side, trying to make notches about the right length.
		int numNotchesW = closestOddTo(width / notchLength);
		int numNotchesH = closestOddTo(height / notchLength);
		int numNotchesD = closestOddTo(depth / notchLength);
		
		// compute exact notch lengths
		float notchLengthW = width / (numNotchesW);
		float notchLengthH = height / (numNotchesH);
		float notchLengthD = depth / (numNotchesD);
	
		//and compute the new width based on that (should be a NO-OP)
		float margin= 0.5f +cutwidth;
		width = numNotchesW*notchLengthW;
		height = numNotchesH*notchLengthH;
		depth = numNotchesD*notchLengthD;
			
		//initialize the eps file
		float boxPiecesWidth = (depth*2+width);		// based on layout of pieces
		float boxPiecesHeight = (height*2+depth*2); // based on layout of pieces
		openDoc((float) (boxPiecesWidth+margin*4),(float) (boxPiecesHeight+margin*5));
        if(specifiedInInches) {
            doc.add(new Paragraph("Width (in): "+width));
            doc.add(new Paragraph("Height (in): "+height));
            doc.add(new Paragraph("Depth (in): "+depth));
            doc.add(new Paragraph("Thickness (in): "+thickness));
            doc.add(new Paragraph("Notch Length (in): "+notchLength));
            doc.add(new Paragraph("Cut Width (in): "+cutwidth));        
        } else {
            doc.add(new Paragraph("Width (mm): "+width * MM_PER_INCH));
            doc.add(new Paragraph("Height (mm): "+height * MM_PER_INCH));
            doc.add(new Paragraph("Depth (mm): "+depth * MM_PER_INCH));
            doc.add(new Paragraph("Thickness (mm): "+thickness * MM_PER_INCH));
            doc.add(new Paragraph("Notch Length (mm): "+notchLength * MM_PER_INCH));
            doc.add(new Paragraph("Cut Width (mm): "+cutwidth * MM_PER_INCH));        
        }
		if(drawBoundingBox) drawBoundingBox(margin,boxPiecesWidth+margin*2,boxPiecesHeight+margin*3,specifiedInInches);

		//start the drawing phase
		float xOrig = 0;
		float yOrig = 0;
	
		// compensate for the cut width (in part) by increasing mwidth (eolson)
		// no, don't do that, because the cut widths cancel out. (eolson)
		//	    mwidth+=cutwidth/2; 

		//1. a W x H side (the back)
		xOrig = depth + margin*2;
		yOrig = margin;
		drawHorizontalLine(xOrig,yOrig,notchLengthW,numNotchesW,thickness,cutwidth/2,false,false);					//top
		drawHorizontalLine(xOrig,yOrig+height-thickness,notchLengthW,numNotchesW,thickness,cutwidth/2,true,false);	//bottom
		drawVerticalLine(xOrig,yOrig,notchLengthH,numNotchesH,thickness,cutwidth/2,false,false);					//left
		drawVerticalLine(xOrig+width-thickness,yOrig,notchLengthH,numNotchesH,thickness,-cutwidth/2,false,false);	//right
		
		//2. a D x H side (the left side)
		xOrig = margin;
		yOrig = height + margin*2;
		drawHorizontalLine(xOrig,yOrig,notchLengthD,numNotchesD,thickness,cutwidth/2,false,false);					//top
		drawHorizontalLine(xOrig,yOrig+height-thickness,notchLengthD,numNotchesD,thickness,cutwidth/2,true,false);	//bottom
		drawVerticalLine(xOrig,yOrig,notchLengthH,numNotchesH,thickness,cutwidth/2,false,false);					//left
		drawVerticalLine(xOrig+depth-thickness,yOrig,notchLengthH,numNotchesH,thickness,-cutwidth/2,false,false);	//right
		
		//3. a W x D side (the bottom)
		xOrig = depth + margin*2;
		yOrig = height + margin*2;
		drawHorizontalLine(xOrig,yOrig,notchLengthW,numNotchesW,thickness,-cutwidth/2,true,true);				//top
		drawHorizontalLine(xOrig,yOrig+depth-thickness,notchLengthW,numNotchesW,thickness,-cutwidth/2,false,true);	//bottom
		drawVerticalLine(xOrig,yOrig,notchLengthD,numNotchesD,thickness,-cutwidth/2,true,true);				//left
		drawVerticalLine(xOrig+width-thickness,yOrig,notchLengthD,numNotchesD,thickness,-cutwidth/2,false,true);	//right

		//4. a D x H side (the right side)
		xOrig = depth + width + margin*3;
		yOrig = height + margin*2;
		drawHorizontalLine(xOrig,yOrig,notchLengthD,numNotchesD,thickness,cutwidth/2,false,false);					//top
		drawHorizontalLine(xOrig,yOrig+height-thickness,notchLengthD,numNotchesD,thickness,cutwidth/2,true,false);	//bottom
		drawVerticalLine(xOrig,yOrig,notchLengthH,numNotchesH,thickness,cutwidth/2,false,false);					//left
		drawVerticalLine(xOrig+depth-thickness,yOrig,notchLengthH,numNotchesH,thickness,-cutwidth/2,false,false);	//right

		//5. a W x H side (the front)
		xOrig = depth + margin*2;
		yOrig = height + depth+ margin*3;
		drawHorizontalLine(xOrig,yOrig,notchLengthW,numNotchesW,thickness,cutwidth/2,false,false);					//top
		drawHorizontalLine(xOrig,yOrig+height-thickness,notchLengthW,numNotchesW,thickness,cutwidth/2,true,false);	//bottom
		drawVerticalLine(xOrig,yOrig,notchLengthH,numNotchesH,thickness,cutwidth/2,false,false);					//left
		drawVerticalLine(xOrig+width-thickness,yOrig,notchLengthH,numNotchesH,thickness,-cutwidth/2,false,false);	//right
		
		//6. a W x D side (the top)
		xOrig = depth + margin*2;
		yOrig = height*2 + depth + margin*4;
		drawHorizontalLine(xOrig,                 yOrig,
                           notchLengthW, numNotchesW, thickness, -cutwidth/2, true,  true); //top
		drawHorizontalLine(xOrig,                 yOrig+depth-thickness,
                           notchLengthW, numNotchesW, thickness, -cutwidth/2, false, true);	//bottom
		drawVerticalLine  (xOrig,                 yOrig,
                           notchLengthD, numNotchesD, thickness, -cutwidth/2, true,  true); //left
		drawVerticalLine  (xOrig+width-thickness, yOrig,
                           notchLengthD, numNotchesD, thickness, -cutwidth/2, false, true);	//right

    }

	/**
     * Draw one horizontal notched line
     * @param x0			x-coord of the starting point of the line (lower left corner) 
     * @param y0			y-coord of the starting point of the line (lower left corner)
     * @param notchWidth	the width of each notch to draw in millimeters
     * @param notchCount	the number of notches to draw along the edge
     * @param notchHeight	the height of the notches to draw (the material thickness)
     * @param cutwidth		the width of the laser beam to compensate for
     * @param flip			should the first line (at x0,y0) be out or in
     * @param smallside		should this stop short of the full height or not
     */
    private void drawHorizontalLine(float x0, float y0,
                                    float notchWidth,
                                    int notchCount,
                                    float notchHeight /*material tickness*/,
                                    float cutwidth,
                                    boolean flip, boolean smallside){
    	float x = x0, y = y0;
    	System.out.println("Horizonal side: "+notchCount+" steps @ ( "+x0+" , "+y0+" )");
    	
        boolean tabs = true;
        int tab1 = -1, tab2 = -1;
        if (tabs) {
            /* Figure out which notch to extend */

            
        }

        for (int step = 0; step < notchCount; step++)
        {
			y=(((step%2)==0)^flip) ? y0 : y0+notchHeight;
	
			if(step==0){		//start first edge in the right place
			    if(smallside) drawLineByMm(x+notchHeight,y,x+notchWidth+cutwidth,y);
			    else drawLineByMm(x,y,x+notchWidth+cutwidth,y);
			} else if (step==(notchCount-1)){	//shorter last edge
			    drawLineByMm(x-cutwidth,y,x+notchWidth-notchHeight,y);
			} else if (step%2==0) {
			    drawLineByMm(x-cutwidth,y,x+notchWidth+cutwidth,y);
		    } else {
			    drawLineByMm(x+cutwidth,y,x+notchWidth-cutwidth,y);
		    }
			
			if (step<(notchCount-1)){
			    if (step%2==0){
					drawLineByMm(x+notchWidth+cutwidth,y0+notchHeight,x+notchWidth+cutwidth,y0);
			    } else {
					drawLineByMm(x+notchWidth-cutwidth,y0+notchHeight,x+notchWidth-cutwidth,y0);
			    }
			}
			
			x=x+notchWidth;
		}
    }

    /**
     * Draw one vertical notched line
     * @param x0			x-coord of the starting point of the line (lower left corner) 
     * @param y0			y-coord of the starting point of the line (lower left corner)
     * @param notchWidth	the width of each notch to draw in millimeters
     * @param notchCount	the number of notches to draw along the edge
     * @param notchHeight	the height of the notches to draw (the material thickness)
     * @param cutwidth		the width of the laser beam to compensate for
     * @param flip			should the first line (at x0,y0) be out or in
     * @param smallside		should this stop short of the full height or not
     */
    private void drawVerticalLine(float x0, float y0,
                                  float notchWidth, int notchCount,
                                  float notchHeight, float cutwidth,
                                  boolean flip, boolean smallside){
		float x=x0,y=y0;
        System.out.println("Vertical side: "+notchCount+" steps @ ( "+x0+" , "+y0+" )");
	
		for (int step=0;step<notchCount;step++) {
			x=(((step%2)==0)^flip) ? x0 : x0+notchHeight;
	
			if (step==0) {
				if(smallside) drawLineByMm(x,y+notchHeight,x,y+notchWidth+cutwidth);
			    else drawLineByMm(x,y,x,y+notchWidth+cutwidth);
			} else if (step==(notchCount-1)) {
			    //g.moveTo(x,y+cutwidth); g.lineTo(x,y+notchWidth); g.stroke();
				if(smallside) drawLineByMm(x,y-cutwidth,x,y+notchWidth-notchHeight);
			    else drawLineByMm(x,y-cutwidth,x,y+notchWidth); 
			} else if (step%2==0) {
			    drawLineByMm(x,y-cutwidth,x,y+notchWidth+cutwidth);
			} else {
			    drawLineByMm(x,y+cutwidth,x,y+notchWidth-cutwidth);
			}
			
			if (step<(notchCount-1)) {
			    if (step%2==0) {
			    	drawLineByMm(x0+notchHeight,y+notchWidth+cutwidth,x0,y+notchWidth+cutwidth);
			    } else {
			    	drawLineByMm(x0+notchHeight,y+notchWidth-cutwidth,x0,y+notchWidth-cutwidth);
			    }
			}
			y=y+notchWidth;
		}
    }

    /**
     * Low-level function to draw lines
     * @param fromXmm	start x pos on age (in millimeters)
     * @param fromYmm	start y pos on age (in millimeters)
     * @param toXmm		end x pos on age (in millimeters)
     * @param toYmm		end y pos on age (in millimeters)
     */
    private void drawLineByMm(float fromXmm,float fromYmm,float toXmm,float toYmm){
    	PdfContentByte cb = docPdfWriter.getDirectContent();
		cb.setLineWidth(0f);
		float x0 = DPI*fromXmm;
		float y0 = DPI*fromYmm;
    	cb.moveTo(x0,y0);
    	float x1 = DPI*toXmm;
    	float y1 = DPI*toYmm;
    	cb.lineTo(x1, y1);
    	cb.stroke();
    	System.out.println(" Line  - ( "+x0+" , "+y0+" ) to ( "+x1+" , "+y1+" )");
    }

    /**
     * Draw a rectangle with based on the endpoints passed in
     * @param fromXmm
     * @param fromYmm
     * @param toXmm
     * @param toYmm
     */
    private void drawBoxByMm(float fromXmm,float fromYmm,float toXmm,float toYmm){
     	PdfContentByte cb = docPdfWriter.getDirectContent();
		cb.setLineWidth(0f);
		float x0 = DPI*fromXmm;
		float y0 = DPI*fromYmm;
    	float x1 = DPI*toXmm;
    	float y1 = DPI*toYmm;
    	cb.rectangle(x0, y0, x1, y1);
    	cb.stroke();
    	System.out.println(" Box  - ( "+x0+" , "+y0+" ) to ( "+x1+" , "+y1+" )");
    }    
}