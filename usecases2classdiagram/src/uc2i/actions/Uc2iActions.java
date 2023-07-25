package uc2i.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.ProjectManager;
import com.vp.plugin.ViewManager;
import com.vp.plugin.action.VPAction;
import com.vp.plugin.action.VPActionController;
import com.vp.plugin.model.IActor;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IAssociationEnd;
import com.vp.plugin.model.IAttribute;
import com.vp.plugin.model.IClass;
import com.vp.plugin.model.IExtend;
import com.vp.plugin.model.IGeneralization;
import com.vp.plugin.model.IInclude;
import com.vp.plugin.model.IModel;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IOperation;
import com.vp.plugin.model.IPackage;
import com.vp.plugin.model.IRealization;
import com.vp.plugin.model.IRelationship;
import com.vp.plugin.model.IRelationshipEnd;
import com.vp.plugin.model.IUseCase;
import com.vp.plugin.model.factory.IModelElementFactory;
import com.vp.plugin.model.property.ITransitProperty;

public class Uc2iActions implements VPActionController {

	@Override
	public void performAction(VPAction arg0) {

		ViewManager viewManager = ApplicationManager.instance().getViewManager();
		viewManager.showMessage("Generating Interfaces from Uses Cases");

		ProjectManager projectManager = ApplicationManager.instance().getProjectManager();

		IPackage interfacePackage = IModelElementFactory.instance().createPackage();
		interfacePackage.setName("interfaz");
		IPackage databasePackage = IModelElementFactory.instance().createPackage();
		databasePackage.setName("basededatos");

		IClass ClassMain = IModelElementFactory.instance().createClass();

		ClassMain.setName("BDPrincipal");

		databasePackage.addChild(ClassMain);

		Map<String, String> actors_id = new HashMap<String, String>();
		Map<String, String> interfaces_id = new HashMap<String, String>();
		Map<String, String> usecase_id = new HashMap<String, String>();
		Map<String, String> externals = new HashMap<String, String>();

		IModelElement[] actors = projectManager.getProject().toModelElementArray(IModelElementFactory.MODEL_TYPE_ACTOR);

		// GENERATION OF CLASS ACTORS IN INTERFACE

		for (int i = 0; i < actors.length; i++) {
			IModelElement modelElement = actors[i];
			IActor actor = (IActor) modelElement;
			IClass Class = IModelElementFactory.instance().createClass();
			interfacePackage.addChild(Class);
			if (actor.hasStereotype("external")) {
				Class.addStereotype("external");
			} else if (actor.hasStereotype("scenario")) {
				Class.addStereotype("scenario");
			} else {
				Class.addStereotype("Actor");
			}

			Class.setName(clean(actor.getName()));
			if (actors_id.containsKey(clean(actor.getName()))) {
				viewManager.showMessage("Warning: Duplicate actor names");
			}
			actors_id.put(clean(actor.getName()), Class.getId());
			// TRANSIT TO
			ITransitProperty transitProp = (ITransitProperty) actor.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
			transitProp.addValue(Class);

		}

		// GENERATION OF INTERFACES IN DATABASE

		for (int i = 0; i < actors.length; i++) {

			IModelElement modelElement = actors[i];
			IActor actor = (IActor) modelElement;
			if (actor.hasStereotype("external")) {
			} else {
				IClass Class = IModelElementFactory.instance().createClass();
				databasePackage.addChild(Class);

				Class.addStereotype("Interface");
				Class.setName("i" + clean(actor.getName()));
				if (interfaces_id.containsKey("i" + clean(actor.getName()))) {
					viewManager.showMessage("Warning: Duplicate actor names");
				}
				interfaces_id.put("i" + clean(actor.getName()), Class.getId());

				IRealization real = IModelElementFactory.instance().createRealization();
				real.setFrom(Class);
				real.setTo(ClassMain);

				String id_from = actors_id.get(clean(actor.getName()));
				IModelElement act_from = projectManager.getProject().getModelElementById(id_from);
				IAssociation association = IModelElementFactory.instance().createAssociation();
				association.setFrom(act_from);
				association.setTo(Class);
				IAssociationEnd associationFromEnd = (IAssociationEnd) association.getFromEnd();
				associationFromEnd.setMultiplicity("1");
				associationFromEnd.setName("_" + clean(act_from.getName()));
				IAssociationEnd associationToEnd = (IAssociationEnd) association.getToEnd();
				associationToEnd.setMultiplicity("1");
				associationToEnd.setName("_" + clean(Class.getName()));
				associationFromEnd.setNavigable(1);
				// TRANSIT TO
				ITransitProperty transitProp = (ITransitProperty) actor.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(Class);
			}

		}

		// GENERATION OF INHERITANCE IN CLASS ACTORS

		for (int i = 0; i < actors.length; i++) {
			IModelElement modelElement = actors[i];
			IActor actor = (IActor) modelElement;
			Iterator<?> genIter = actor.fromRelationshipIterator();

			while (genIter.hasNext()) {

				IRelationship relationship = (IRelationship) genIter.next();
				IModelElement toactor = relationship.getTo();
				IGeneralization generalizationModel = IModelElementFactory.instance().createGeneralization();
				String id_from = actors_id.get(clean(actor.getName()));
				IModelElement act_from = projectManager.getProject().getModelElementById(id_from);
				String id_to = actors_id.get(clean(toactor.getName()));
				IModelElement act_to = projectManager.getProject().getModelElementById(id_to);
				generalizationModel.setFrom(act_from);
				generalizationModel.setTo(act_to);

				// TRANSIT TO
				ITransitProperty transitProp = (ITransitProperty) relationship
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(generalizationModel);

			}

		}

		// GENERATION OF INHERITANCE IN INTERFACES

		for (int i = 0; i < actors.length; i++) {
			IModelElement modelElement = actors[i];
			IActor actor = (IActor) modelElement;
			Iterator<?> genIter = actor.fromRelationshipIterator();

			while (genIter.hasNext()) {

				IRelationship relationship = (IRelationship) genIter.next();
				IModelElement toactor = relationship.getTo();
				IGeneralization generalizationModel = IModelElementFactory.instance().createGeneralization();
				String id_from = interfaces_id.get("i" + clean(actor.getName()));
				IModelElement act_from = projectManager.getProject().getModelElementById(id_from);
				String id_to = interfaces_id.get("i" + clean(toactor.getName()));
				IModelElement act_to = projectManager.getProject().getModelElementById(id_to);
				generalizationModel.setFrom(act_from);
				generalizationModel.setTo(act_to);

				// TRANSIT TO
				ITransitProperty transitProp = (ITransitProperty) relationship
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(generalizationModel);

			}

		}

		// GENERATION OF USE CASES

		IModelElement[] usecases = projectManager.getProject()
				.toModelElementArray(IModelElementFactory.MODEL_TYPE_USE_CASE);

		for (int i = 0; i < usecases.length; i++) {
			IModelElement modelElement = usecases[i];
			IUseCase useCase = (IUseCase) modelElement;

			if (useCase.hasStereotype("event")) {
			} else if (useCase.hasStereotype("business")) {
			} else {
				IClass Class = IModelElementFactory.instance().createClass();
				interfacePackage.addChild(Class);
				Class.setName(clean(useCase.getName()));
				 
				
				
				if (usecase_id.containsKey(clean(useCase.getName()))) {
					viewManager.showMessage("Warning: Duplicate use case names");
				}
				usecase_id.put(clean(useCase.getName()), Class.getId());
				if (useCase.hasStereotype("list")) {
					
					

					IClass ClassItem = IModelElementFactory.instance().createClass();
					interfacePackage.addChild(ClassItem);
					ClassItem.setName(clean(useCase.getName()) + "_item");
					usecase_id.put(clean(useCase.getName()) + "_item", ClassItem.getId());
					
					Class.addStereotype("list");
					ITransitProperty transitProp1 = (ITransitProperty) useCase
							.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					transitProp1.addValue(ClassItem);
					
					 
					if (isRoot(modelElement)) {

					IAssociation association = IModelElementFactory.instance().createAssociation();
					association.setFrom(Class);
					association.setTo(ClassItem);

					IAssociationEnd associationFromEnd = (IAssociationEnd) association.getFromEnd();
					//associationFromEnd.setMultiplicity("1");
					associationFromEnd.setName("_" + Class.getName());

					IAssociationEnd associationToEnd = (IAssociationEnd) association.getToEnd();
					associationToEnd.setMultiplicity("*");
					associationToEnd.setName("_item");

					associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
					associationFromEnd.setNavigable(1);
					}
							
					
					// ITransitProperty transitProp2 = (ITransitProperty)
					// useCase.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					// transitProp2.addValue(association);
					
					
					 
					
					
					
					
					
				}
				ITransitProperty transitProp = (ITransitProperty) useCase
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(Class);
			}
		}

		// GENERATION OF INHERITANCE OF USE CASES

		for (int i = 0; i < usecases.length; i++) {
			IModelElement modelElement = usecases[i];
			IUseCase usecase = (IUseCase) modelElement;
			Iterator<?> genIter = usecase.fromRelationshipIterator();

			while (genIter.hasNext()) {

				IRelationship relationship = (IRelationship) genIter.next();
				if (relationship.getModelType() == "Generalization") {
					IModelElement tousecase = relationship.getTo();
					IGeneralization generalizationModel = IModelElementFactory.instance().createGeneralization();
					String id_from = usecase_id.get(clean(usecase.getName()));
					IModelElement use_from = projectManager.getProject().getModelElementById(id_from);
					String id_to = usecase_id.get(clean(tousecase.getName()));
					IModelElement use_to = projectManager.getProject().getModelElementById(id_to);
					generalizationModel.setFrom(use_from);
					generalizationModel.setTo(use_to);
					// TRANSIT TO
					ITransitProperty transitProp = (ITransitProperty) relationship
							.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					transitProp.addValue(generalizationModel);

					// INHERITANCE IN ITEMS

					if (usecase.hasStereotype("list")) {
						IGeneralization generalizationModelitem = IModelElementFactory.instance()
								.createGeneralization();
						String id_fromitem = usecase_id.get(clean(usecase.getName()) + "_item");
						IModelElement use_fromitem = projectManager.getProject().getModelElementById(id_fromitem);
						String id_toitem = usecase_id.get(clean(tousecase.getName()) + "_item");
						IModelElement use_toitem = projectManager.getProject().getModelElementById(id_toitem);
						generalizationModelitem.setFrom(use_fromitem);
						generalizationModelitem.setTo(use_toitem);
						// TRANSIT TO
						ITransitProperty transitProp2 = (ITransitProperty) relationship
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp2.addValue(generalizationModelitem);
					}

				}

			}

		}

		// GENERATION OF ASSOCIATIONS OF ACTORS AND USE CASES

		for (int i = 0; i < actors.length; i++) {
			IModelElement modelElement = actors[i];
			IActor fromactor = (IActor) modelElement;
			Iterator<?> genIter = fromactor.fromRelationshipEndIterator();

			while (genIter.hasNext()) {

				IRelationshipEnd relationshipEnd = (IRelationshipEnd) genIter.next();
				IModelElement tousecase = relationshipEnd.getEndRelationship().getTo();

				String list = "";
				if (fromactor.hasStereotype("list")) {
					if (tousecase.hasStereotype("option")) {
						list = "";
					} else {
						list = "_item";
					}
				}

				if (tousecase.hasStereotype("event")) {

					if (fromactor.hasStereotype("external")) {
						String id_from = actors_id.get(clean(fromactor.getName()) + list);
						externals.put(clean(tousecase.getName()), id_from);
						IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
						IOperation event_handler = IModelElementFactory.instance().createOperation();
						event_handler.setName(clean(tousecase.getName()));
						event_handler.setReturnType("void");
						actor_from.addOperation(event_handler);
						// TRANSIT TO
						ITransitProperty transitProp = (ITransitProperty) tousecase
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp.addValue(event_handler);
					} else {
						String id_from = actors_id.get(clean(fromactor.getName()) + list);
						IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
						IAttribute attribute = IModelElementFactory.instance().createAttribute();
						attribute.setName(clean(tousecase.getName()));
						attribute.setType("event");
						actor_from.addAttribute(attribute);
						IOperation event_handler = IModelElementFactory.instance().createOperation();
						event_handler.setName(clean(tousecase.getName()));
						event_handler.setReturnType("void");
						actor_from.addOperation(event_handler);
						// TRANSIT TO
						ITransitProperty transitProp = (ITransitProperty) tousecase
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp.addValue(attribute);
						transitProp.addValue(event_handler);
					}

				} else if (tousecase.hasStereotype("business")) {
					String id_from = actors_id.get(clean(fromactor.getName()) + list);
					IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
					IOperation event_handler = IModelElementFactory.instance().createOperation();
					event_handler.setName(clean(tousecase.getName()));
					event_handler.setReturnType("void");
					actor_from.addOperation(event_handler);
					// TRANSIT TO
					ITransitProperty transitProp = (ITransitProperty) tousecase
							.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					transitProp.addValue(event_handler);

					if (fromactor.hasStereotype("external")) {
						externals.put(clean(tousecase.getName()), id_from);
					}

				}

				else {

					IAssociation nassociation = IModelElementFactory.instance().createAssociation();

					String id_from = actors_id.get(clean(fromactor.getName()) + list);
					IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
					String id_to = usecase_id.get(clean(tousecase.getName()));
					IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(id_to);

					if (fromactor.hasStereotype("external")) {
						externals.put(clean(tousecase.getName()), id_from);
					} else {
						IOperation event_handler = IModelElementFactory.instance().createOperation();
						event_handler.setName(clean(tousecase.getName()));
						event_handler.setReturnType("void");
						actor_from.addOperation(event_handler);
						ITransitProperty transitProp = (ITransitProperty) tousecase
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp.addValue(event_handler);
					}

					nassociation.setFrom(actor_from);
					nassociation.setTo(usecase_to);

					if (fromactor.hasStereotype("external")) {
						IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
						associationFromEnd.setMultiplicity("1");
						associationFromEnd.setName("_" + clean(actor_from.getName()));
						IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
						associationToEnd.setMultiplicity("1");
						associationToEnd.setName("_" + clean(tousecase.getName()));
						// associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
						associationToEnd.setNavigable(1);
						// TRANSIT TO
						// ITransitProperty transitProp2 = (ITransitProperty)
						// relationshipEnd.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						// transitProp2.addValue(nassociation);

					} else {
						IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
						//associationFromEnd.setMultiplicity("1");
						associationFromEnd.setName("_" + clean(actor_from.getName()));
						IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
						associationToEnd.setMultiplicity("1");
						associationToEnd.setName("_" + clean(tousecase.getName()));
						associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
						associationFromEnd.setNavigable(1);
						// TRANSIT TO
						// ITransitProperty transitProp2 = (ITransitProperty)
						// relationshipEnd.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						// transitProp2.addValue(nassociation);
					}
				}

			}
		}

		for (int i = 0; i < actors.length; i++) {
			IModelElement modelElement = actors[i];
			IActor fromactor = (IActor) modelElement;
			Iterator<?> genIter = fromactor.toRelationshipEndIterator();

			while (genIter.hasNext()) {

				IRelationshipEnd relationshipEnd = (IRelationshipEnd) genIter.next();
				IModelElement tousecase = relationshipEnd.getEndRelationship().getFrom();

				String list = "";
				if (fromactor.hasStereotype("list")) {
					if (tousecase.hasStereotype("option")) {
						list = "";
					} else {
						list = "_item";
					}
				}

				if (tousecase.hasStereotype("event")) {

					if (fromactor.hasStereotype("external")) {
						String id_from = actors_id.get(clean(fromactor.getName()) + list);
						externals.put(clean(tousecase.getName()), id_from);
						IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
						IOperation event_handler = IModelElementFactory.instance().createOperation();
						event_handler.setName(clean(tousecase.getName()));
						event_handler.setReturnType("void");
						actor_from.addOperation(event_handler);
						// TRANSIT TO
						ITransitProperty transitProp = (ITransitProperty) tousecase
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp.addValue(event_handler);
					} else {

						String id_from = actors_id.get(clean(fromactor.getName()) + list);
						IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
						IAttribute attribute = IModelElementFactory.instance().createAttribute();
						attribute.setName(clean(tousecase.getName()));
						attribute.setType("event");
						actor_from.addAttribute(attribute);
						IOperation event_handler = IModelElementFactory.instance().createOperation();
						event_handler.setName(clean(tousecase.getName()));
						event_handler.setReturnType("void");
						actor_from.addOperation(event_handler);
						// TRANSIT TO
						ITransitProperty transitProp = (ITransitProperty) tousecase
								.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						transitProp.addValue(attribute);
						transitProp.addValue(event_handler);
					}

				} else if (tousecase.hasStereotype("business")) {
					String id_from = actors_id.get(clean(fromactor.getName()) + list);
					IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
					IOperation event_handler = IModelElementFactory.instance().createOperation();
					event_handler.setName(clean(tousecase.getName()));
					event_handler.setReturnType("void");
					actor_from.addOperation(event_handler);
					// TRANSIT TO
					ITransitProperty transitProp = (ITransitProperty) tousecase
							.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					transitProp.addValue(event_handler);
					if (fromactor.hasStereotype("external")) {
						externals.put(clean(tousecase.getName()), id_from);
					}

				}

				else {

					IAssociation nassociation = IModelElementFactory.instance().createAssociation();

					String id_from = actors_id.get(clean(fromactor.getName()) + list);
					IClass actor_from = (IClass) projectManager.getProject().getModelElementById(id_from);
					String id_to = usecase_id.get(clean(tousecase.getName()));
					IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(id_to);

					if (fromactor.hasStereotype("external")) {
						externals.put(clean(tousecase.getName()), id_from);
					}

					nassociation.setFrom(actor_from);
					nassociation.setTo(usecase_to);

					IOperation event_handler = IModelElementFactory.instance().createOperation();
					event_handler.setName(clean(tousecase.getName()));
					event_handler.setReturnType("void");
					actor_from.addOperation(event_handler);
					ITransitProperty transitProp = (ITransitProperty) tousecase
							.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
					transitProp.addValue(event_handler);

					if (fromactor.hasStereotype("external")) {
						IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
						associationFromEnd.setMultiplicity("1");
						associationFromEnd.setName("_" + clean(actor_from.getName()));
						IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
						associationToEnd.setMultiplicity("1");
						associationToEnd.setName("_" + clean(tousecase.getName()));
						// associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
						associationToEnd.setNavigable(1);
						// TRANSIT TO
						// ITransitProperty transitProp2 = (ITransitProperty)
						// relationshipEnd.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						// transitProp2.addValue(nassociation);

					} else {

						IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
						//associationFromEnd.setMultiplicity("1");
						associationFromEnd.setName("_" + clean(actor_from.getName()));
						IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
						associationToEnd.setMultiplicity("1");
						associationToEnd.setName("_" + clean(tousecase.getName()));
						associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
						associationFromEnd.setNavigable(1);
						// TRANSIT TO
						// ITransitProperty transitProp2 = (ITransitProperty)
						// relationshipEnd.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
						// transitProp2.addValue(nassociation);
					}
				}

			}
		}

		// GENERATION OF ASSOCIATIONS, EVENTS AND LISTS OF USE CASES

		IModelElement[] includes = projectManager.getProject()
				.toAllLevelModelElementArray(IModelElementFactory.MODEL_TYPE_INCLUDE);

		for (int i = 0; i < includes.length; i++) {

			IModelElement modelElement = includes[i];
			IInclude include = (IInclude) modelElement;

			IModelElement fromusecase = include.getFrom();
			IModelElement tousecase = include.getTo();

			String list = "";
			if (fromusecase.hasStereotype("list")) {
				if (tousecase.hasStereotype("option")) {
					list = "";
				} else {
					list = "_item";
				}
			}

			if (tousecase.hasStereotype("event")) {

				if (externals.containsKey(clean(tousecase.getName()))) {
					String extern = externals.get(clean(tousecase.getName()));
					IAssociation nassociation = IModelElementFactory.instance().createAssociation();

					String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
					IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);

					IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(extern);

					nassociation.setFrom(usecase_from);
					nassociation.setTo(usecase_to);
					IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
					associationFromEnd.setMultiplicity("1");
					associationFromEnd.setName("_" + clean(usecase_from.getName()));
					IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
					associationToEnd.setMultiplicity("1");
					associationToEnd.setName("_" + clean(usecase_to.getName()));
					// associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
					associationFromEnd.setNavigable(1);
				}

				String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
				IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);
				IAttribute attribute = IModelElementFactory.instance().createAttribute();
				attribute.setName(clean(tousecase.getName()));
				attribute.setType("event");
				usecase_from.addAttribute(attribute);
				IOperation event_handler = IModelElementFactory.instance().createOperation();
				event_handler.setName(clean(tousecase.getName()));
				event_handler.setReturnType("void");
				usecase_from.addOperation(event_handler);
				// TRANSIT TO
				ITransitProperty transitProp = (ITransitProperty) tousecase
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(attribute);
				transitProp.addValue(event_handler);

			} else if (tousecase.hasStereotype("business")) {

				if (externals.containsKey(clean(tousecase.getName()))) {
					String extern = externals.get(clean(tousecase.getName()));
					IAssociation nassociation = IModelElementFactory.instance().createAssociation();

					String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
					IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);

					IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(extern);

					nassociation.setFrom(usecase_from);
					nassociation.setTo(usecase_to);
					IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
					associationFromEnd.setMultiplicity("1");
					associationFromEnd.setName("_" + clean(usecase_from.getName()));
					IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
					associationToEnd.setMultiplicity("1");
					associationToEnd.setName("_" + clean(usecase_to.getName()));
					// associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
					associationFromEnd.setNavigable(1);
				}

				String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
				IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);
				IOperation event_handler = IModelElementFactory.instance().createOperation();
				event_handler.setName(clean(tousecase.getName()));
				event_handler.setReturnType("void");
				usecase_from.addOperation(event_handler);
				// TRANSIT TO
				ITransitProperty transitProp = (ITransitProperty) tousecase
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(event_handler);

			}

			else {

				if (externals.containsKey(clean(tousecase.getName()))) {
					String extern = externals.get(clean(tousecase.getName()));
					IAssociation nassociation = IModelElementFactory.instance().createAssociation();

					String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
					IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);

					IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(extern);

					nassociation.setFrom(usecase_from);
					nassociation.setTo(usecase_to);
					IAssociationEnd associationFromEnd = (IAssociationEnd) nassociation.getFromEnd();
					associationFromEnd.setMultiplicity("1");
					associationFromEnd.setName("_" + clean(usecase_from.getName()));
					IAssociationEnd associationToEnd = (IAssociationEnd) nassociation.getToEnd();
					associationToEnd.setMultiplicity("1");
					associationToEnd.setName("_" + clean(usecase_to.getName()));
					// associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
					associationFromEnd.setNavigable(1);
				}

				IAssociation association = IModelElementFactory.instance().createAssociation();

				String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
				IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);
				String id_to = usecase_id.get(clean(tousecase.getName()));
				IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(id_to);

				association.setFrom(usecase_from);
				association.setTo(usecase_to);

				IOperation event_handler = IModelElementFactory.instance().createOperation();
				event_handler.setName(clean(tousecase.getName()));
				event_handler.setReturnType("void");
				usecase_from.addOperation(event_handler);
				
				
				
				ITransitProperty transitProp = (ITransitProperty) tousecase
						.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				transitProp.addValue(event_handler);

				IAssociationEnd associationFromEnd = (IAssociationEnd) association.getFromEnd();
				//associationFromEnd.setMultiplicity("1");
				associationFromEnd.setName("_" + clean(fromusecase.getName()));
				IAssociationEnd associationToEnd = (IAssociationEnd) association.getToEnd();
				associationToEnd.setMultiplicity("1");
				associationToEnd.setName("_" + clean(tousecase.getName()));
				associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
				associationFromEnd.setNavigable(1);
				// TRANSIT TO
				// ITransitProperty transitProp2 = (ITransitProperty)
				// include.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
				// transitProp2.addValue(association);

			}

		}

		// CASE EXTENDS

		IModelElement[] extend = projectManager.getProject()
				.toAllLevelModelElementArray(IModelElementFactory.MODEL_TYPE_EXTEND);

		for (int i = 0; i < extend.length; i++) {
			IModelElement modelElement = extend[i];
			IExtend extendi = (IExtend) modelElement;

			IModelElement fromusecase = extendi.getFrom();
			IModelElement tousecase = extendi.getTo();

			String list = "";
			if (fromusecase.hasStereotype("list")) {
				if (tousecase.hasStereotype("option")) {
					list = "";
				} else {
					list = "_item";
				}
			}

			IAssociation association = IModelElementFactory.instance().createAssociation();
			String id_from = usecase_id.get(clean(fromusecase.getName()) + list);
			IClass usecase_from = (IClass) projectManager.getProject().getModelElementById(id_from);
			String id_to = usecase_id.get(clean(tousecase.getName()));
			IClass usecase_to = (IClass) projectManager.getProject().getModelElementById(id_to);

			association.setFrom(usecase_from);
			association.setTo(usecase_to);

			usecase_to.addStereotype("extends");
			IAssociationEnd associationFromEnd = (IAssociationEnd) association.getFromEnd();
			//associationFromEnd.setMultiplicity("1");
			associationFromEnd.setName("_" + clean(fromusecase.getName()));
			IAssociationEnd associationToEnd = (IAssociationEnd) association.getToEnd();
			associationToEnd.setMultiplicity("1");
			associationToEnd.setName("_" + clean(tousecase.getName()));
			associationFromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
			associationFromEnd.setNavigable(1);
			// TRANSIT TO
			// ITransitProperty transitProp = (ITransitProperty)
			// extendi.getModelPropertyByName(IModel.PROP_TRANSIT_TO);
			// transitProp.addValue(association);

		}

	}
	
	public Boolean isRoot(IModelElement modelElement) {
		
		Boolean root=true;
		
		IUseCase usecase = (IUseCase) modelElement;
		Iterator<?> genIter = usecase.toRelationshipIterator();

		while (genIter.hasNext()) {

			IRelationship relationship = (IRelationship) genIter.next();
			if (relationship.getModelType() == "Generalization") {
				
				root=false;
				
			}
		}
		return root;
		
	}
	
	public String clean(String s) {
		
		s = s.replaceAll("[^a-zA-Z0-9]", "");  
		return s;
	}

	@Override
	public void update(VPAction arg0) {
		// TODO Auto-generated method stub

	}

}
