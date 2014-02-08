package edu.berkeley.nlp.syntax

import scala.collection.JavaConverters._
import scala.collection.Iterator
import scala.collection.immutable.List
import scala.collection.immutable.Set

import TreeConversions._

object Environment { 

  val conditionTags : Set[String] = Set[String]("MD")
  val dependencyTags : Set[String] = Set[String]("IN")
  val clauseTags : Set[String] = Set[String]("S", "SBAR")
  val vpTags : Set[String] = Set[String]("VP")
  val topicTags : Set[String] = Set[String]("NP", "CC")
  val actionTags : Set[String] = Set[String]("VB", "VBZ", "VBP", "VBD", "CC")

  abstract class Term
  case class Action(value : String, dependency : Option[Dependency]) extends Term // VB, VBZ, VBP, VBD, CC
  case class Dependency(value : String, clause : Option[Topic]) extends Term
  case class Condition(modal : String, actionResult: Option[Action]) extends Term
  case class Topic(values : List[String], ability : Option[Term]) extends Term //VGD, NP, CC 
  //TODO: Add support for multiple actions: e.g. The dog can walk and might run.
  // Possible expand a sentence before passing it to the environment.

  /** 
    * Takes a current node assumed to contain a valid subtree produces a topic.
    * Warning: Only a topic can be extracted from sentences of the form 
    * "The dog walks" or the "The dog walks and eats"  because it is like saying 
    * "The dog, who walks and eats". 
    **/
  def toTopic(tree : LinguisticTree) : Option[Topic] = tree.findCut(topicTags) map { 
    (t : LinguisticTree) => Topic(t.terminalList(), toCondition(tree).orElse(toAction(tree)))
  } 

  def toDependency(tree : LinguisticTree) : Option[Dependency] = tree.findCut(dependencyTags) map { 
    (t : LinguisticTree) => Dependency(t.terminalList().mkString(""), toTopic(tree))
  }

  def toAction(tree : LinguisticTree) : Option[Action] = for { 
    t1 <- tree.findCut(vpTags)
    t2 <- t1.findCut(actionTags) 
    action <- Some(Action(t2.terminalList().mkString(""), toDependency(t1)))
  } yield action


  def toCondition(tree : LinguisticTree) : Option[Condition] = for { 
    t1 <- tree.findCut(vpTags)
    t2 <- t1.findCut(conditionTags) 
    condition <- Some(Condition(t2.terminalList().mkString(""), toAction(t1)))
  } yield condition

  def toClause(tree : LinguisticTree) : Option[Term] = tree.iterator().asScala find { 
    (t : LinguisticTree) => clauseTags contains { t.getLabel() } 
  } flatMap { 
    (t : LinguisticTree) => t.getLabel() match { 
      case "S" => toTopic(t)
      case "SBAR" => toDependency(t)
    } 
  } orElse(toTopic(tree))  

} 
 