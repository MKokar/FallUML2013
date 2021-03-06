package compareAlgorithm.smartPolicy;

import java.io.IOException;
import java.util.Properties;

import controller.diagramparser.ClassDiagramParser;
import controller.diagramparser.DiagramParser;
import domain.Attributes;
import domain.CD_Class;
import domain.Classes;
import domain.DiagramPolicyScore;
import domain.Metrics;
import domain.MetricsType;
import domain.Policy;


public class ClassDiagramScoreGenerator {

	private Policy policy;
	private ClassDiagramParser diagramParser;
	private int classScore;
	private int attributesScore;
	private int assicoationScore;
	private int multiplicityScore;
	private int finalScore;
	private DiagramPolicyScore policyScore;
	private String justification;
	
	//Class weights/points
	private int totalClassBetweenPoints;
	private int totalClassMaxPoints;
	private int totalClassOverMaxPoints;
	private int totalClassUnderPoints;
	private int totalClassMinPoints;
	
	//Attribute weights/points
	private int avgAttributesBetweenPoints;
	private int avgAttributesMaxPoints;
	private int avgAttributesOverMaxPoints;
	private int avgAttributesUnderMinPoints;
	private int avgAttributesMinPoints;
	
	protected ClassDiagramScoreGenerator(Policy policyApplied, ClassDiagramParser parser) throws IOException
	{
		policy = policyApplied;
		diagramParser = parser;
		policyScore = new DiagramPolicyScore();
		policyScore.setDiagramID(diagramParser.getDiagram().getDiagramId());
		policyScore.setPolicyID(policy.getPolicyID());
		initPoints();
					
	}
	
	//initialize by reading from a properties file, the values of the various weights assoiated
	private void initPoints() throws IOException
	{
		Properties prop = new Properties();
		prop.load(ClassDiagramScoreGenerator.class.getClassLoader().getResourceAsStream("clubUMLProperties.properties"));	 
		
		totalClassBetweenPoints = Integer.parseInt(prop.getProperty("CLASSBETWEENPOINTS")); 
		totalClassMaxPoints = Integer.parseInt(prop.getProperty("CLASSMAXPOINTS"));
		totalClassUnderPoints = Integer.parseInt(prop.getProperty("CLASSUNDERPOINTS"));
		totalClassOverMaxPoints = Integer.parseInt(prop.getProperty("CLASSOVERMAXPOINTS"));
		totalClassMinPoints = Integer.parseInt(prop.getProperty("CLASSMINPOINTS"));
		
		avgAttributesBetweenPoints = Integer.parseInt(prop.getProperty("ATTRIBUTEBETWEENPOINTS"));
		avgAttributesMaxPoints = Integer.parseInt(prop.getProperty("ATTRIBUTEMAXPOINTS"));
		avgAttributesOverMaxPoints = Integer.parseInt(prop.getProperty("ATTRIBUTEUNDERPOINTS"));
		avgAttributesUnderMinPoints = Integer.parseInt(prop.getProperty("ATTRIBUTEOVERMAXPOINTS"));
		avgAttributesMinPoints = Integer.parseInt(prop.getProperty("ATTRIBUTEMINPOINTS"));
	}
	
	public DiagramPolicyScore generateScore() throws Exception{
		justification = "";
		for(Metrics mObj : policy.getMetrics())
		{
			policyScore.setPolicyScore(GenerateMetricScore(diagramParser, mObj));
			if(justification != "")
				justification += "\n";
			policyScore.setJustification(justification);
		}
		return policyScore;
	}
	
	private int GenerateMetricScore(DiagramParser diagramParser, Metrics mObj)throws Exception{
		
		switch (mObj.getMetricsType()){
		
			case CLASSES:
				classScore = scoreClasses(mObj);
				break;
			case ATTRIBUTES:
				attributesScore = scoreAttributes(mObj);
				break;
			case MULTIPLICITIES:
				multiplicityScore = scoreMultiplicity(mObj);
				break;
			case ASSOCIATIONS:
				assicoationScore = scoreAssociations(mObj);
				break;
			case NOTDEFINED:
		
		}
		
		
		return aggregateScores();
	}
	
	public int aggregateScores(){
		
		finalScore = classScore + attributesScore + multiplicityScore + assicoationScore; //TO DO - Add weights
		//Logic to total Score
		return finalScore;
		
	}
	
	private int scoreClasses(Metrics mObj) throws Exception {
		int score = 0;
		if(mObj.getMetricsType() != MetricsType.CLASSES)
			throw new Exception("Wrong Matrics Type");
		int totalNoOfClasses = diagramParser.getClasses().size();
		int idealNoOfClasses = ((Classes)mObj).getIdealNoOfClasses();
		int maxNoOfClasses = ((Classes)mObj).getMaxNoOfClasses();
		int minNoOfClasses = ((Classes)mObj).getMinNoOfClasses();
		

		if((totalNoOfClasses > idealNoOfClasses && totalNoOfClasses < maxNoOfClasses) || 
				(totalNoOfClasses < idealNoOfClasses && totalNoOfClasses > minNoOfClasses))
		{
			score = Math.abs((idealNoOfClasses - totalNoOfClasses)) * totalClassBetweenPoints;
			if(totalNoOfClasses > idealNoOfClasses) 
				justification += "The number of classes is above the ideal amount set for this Context. But it is less then the Maxinum allowed. ";
			else
				justification += "The number of classes is below the ideal amount set for this Context. But it is greater then the minimum allowed. ";
		}
		else if(diagramParser.getClasses().size() == ((Classes)mObj).getIdealNoOfClasses())
		{
			score = 0;
			justification += "The number of classes is same as the ideal number set for this context.";
		}
		else //overflow
		{
			if(totalNoOfClasses >= maxNoOfClasses){
				score = (totalClassMaxPoints + ((totalNoOfClasses - maxNoOfClasses) * totalClassOverMaxPoints));
				justification += "The number of classes is above the Maxinum allowed for this Context.";
			}
			else if(totalNoOfClasses <= minNoOfClasses){
				score = totalClassMinPoints + ((minNoOfClasses - totalNoOfClasses) * totalClassUnderPoints);
				justification += "The number of classes is less then or equal to the minimum allowed for this context. ";
			}
			else
				throw new Exception("The ideal value is not in between Max and min values.");
			
		}
		return score;
	}
	
	private int scoreAttributes(Metrics mObj) throws Exception{
		int score = 0;
		int avgNoAttribute = 0;
		int totalNoAttributes = 0;
		int idealNoOfAttribuutes = ((Attributes)mObj).getIdealNoOfAttributes();
		int maxNoAttributes = ((Attributes)mObj).getMaxNoOfAttributes();
		int minNoAttributes = ((Attributes)mObj).getMinNoOfAttributes();
		
		int totalNoOfClasses = diagramParser.getClasses().size();
		
		for(CD_Class cdClass : diagramParser.getClasses()){
			totalNoAttributes += cdClass.getAttributes().size();
		}
		
		if(totalNoOfClasses > 0)
			avgNoAttribute = totalNoAttributes/totalNoOfClasses;
		
		if(avgNoAttribute == idealNoOfAttribuutes){
			score = 0;
		}
		else if(((avgNoAttribute > idealNoOfAttribuutes) && (avgNoAttribute < maxNoAttributes)) ||
				((avgNoAttribute < idealNoOfAttribuutes) && (avgNoAttribute > minNoAttributes))){
			score = Math.abs((idealNoOfAttribuutes - avgNoAttribute)) * avgAttributesBetweenPoints;
			if(avgNoAttribute > idealNoOfAttribuutes)
				justification += "The average number of attributes is above the ideal amount set for this Context. But it is less then the Maxinum allowed. ";
			else
				justification += "The average number of attributes is below the ideal amount set for this Context. But it is greater then the minimum allowed. ";
		}
		else //overflow
		{
			if(avgNoAttribute >= maxNoAttributes){
				score = (avgAttributesMaxPoints  + ((avgNoAttribute - maxNoAttributes) * avgAttributesOverMaxPoints ));
				justification += "The average number of attributes is above the Maxinum allowed for this Context.";
			}
			else if(avgNoAttribute <= minNoAttributes){
				score = (avgAttributesMinPoints + ((minNoAttributes - avgNoAttribute) * avgAttributesUnderMinPoints));
				justification += "The average number of Attrubutes less then or equal to the minimum allowed for this context. ";
			}
			else if(avgNoAttribute == 0)
			{
				score = (avgAttributesMinPoints + ((minNoAttributes - avgNoAttribute) * avgAttributesUnderMinPoints));
				justification += "The average number of attributes per class is zero.";
			}
			else
				throw new Exception("The ideal value is not in between Max and min values.");
			
		}	
		
		return score;
	}
	
	private int scoreMultiplicity(Metrics mObj){
		int score = 0;
		//Code here
		return score;
	}
	
	private int scoreAssociations(Metrics mObj){
		int score = 0;
		//Code here
		return score;
	}
}
