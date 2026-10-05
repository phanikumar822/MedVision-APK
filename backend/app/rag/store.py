import os
from typing import List, Dict, Any, Optional
from app.core.config import settings

try:
    import chromadb
    from chromadb.config import Settings as ChromaSettings
    CHROMA_AVAILABLE = True
except ImportError:
    CHROMA_AVAILABLE = False


class MedicalVectorStore:
    """
    ChromaDB vector store managing clinical reports, findings,
    and diabetic retinopathy educational knowledge base.
    """
    def __init__(self):
        self.persist_dir = str(settings.CHROMA_PERSIST_DIR)
        self.fallback_docs: Dict[str, Dict[str, Any]] = {}
        if CHROMA_AVAILABLE:
            try:
                self.client = chromadb.PersistentClient(path=self.persist_dir)
                self.collection = self.client.get_or_create_collection(
                    name="medvision_records",
                    metadata={"description": "MedVisionAI Clinical Diagnostic Records & Patient RAG"}
                )
            except Exception:
                self.client = None
                self.collection = None
        else:
            self.client = None
            self.collection = None
        self._seed_medical_knowledge()


    def _seed_medical_knowledge(self):
        """Seed standard clinical guidelines for DR and Grad-CAM."""
        knowledge_docs = [
            (
                "guide_dr_overview",
                "Diabetic Retinopathy (DR) is a microvascular complication of diabetes caused by chronic high blood glucose levels damaging retinal capillaries. It progresses from Non-Proliferative Diabetic Retinopathy (NPDR) with microaneurysms, hemorrhages, and exudates to Proliferative Diabetic Retinopathy (PDR) with retinal neovascularization and vitreous hemorrhage risk.",
                {"type": "clinical_guideline", "topic": "dr_overview"}
            ),
            (
                "guide_gradcam_explanation",
                "Grad-CAM (Gradient-weighted Class Activation Mapping) produces visual heatmaps identifying pixels that influenced the EfficientNet model's diagnosis. Red, orange, and yellow highlight high-activation lesion zones like microaneurysms, lipid exudates, and intraretinal blot hemorrhages. Blue and green indicate normal parenchyma.",
                {"type": "clinical_guideline", "topic": "gradcam"}
            ),
            (
                "guide_risk_recommendations",
                "High Risk DR requires prompt Vitreoretinal consultation within 1 to 2 weeks for fluorescein angiography and potential anti-VEGF or laser photocoagulation. Moderate Risk requires dilated ophthalmology exam within 4-8 weeks. Low Risk (No DR) advises annual re-screening and glycemic HbA1c control below 7.0%.",
                {"type": "clinical_guideline", "topic": "recommendations"}
            )
        ]
        for doc_id, text, metadata in knowledge_docs:
            if self.collection:
                try:
                    self.collection.upsert(
                        ids=[doc_id],
                        documents=[text],
                        metadatas=[metadata]
                    )
                except Exception:
                    pass
            self.fallback_docs[doc_id] = {"text": text, "metadata": metadata}

    def index_screening_report(
        self,
        patient_id: int,
        screening_id: str,
        patient_name: str,
        prediction: str,
        confidence: float,
        risk_level: str,
        recommendation: str,
        ai_context: str
    ) -> None:
        """Indexes a completed diagnostic screening into ChromaDB."""
        doc_id = f"report_{screening_id}"
        document_text = (
            f"Screening Report ID: {screening_id}\n"
            f"Patient: {patient_name} (Patient ID: {patient_id})\n"
            f"Prediction: {prediction} (Confidence: {confidence * 100:.1f}%)\n"
            f"Risk Level: {risk_level}\n"
            f"Grad-CAM Explainability Context: {ai_context}\n"
            f"Clinical Recommendation: {recommendation}\n"
        )
        metadata = {
            "patient_id": str(patient_id),
            "screening_id": screening_id,
            "prediction": prediction,
            "risk_level": risk_level,
            "type": "patient_screening"
        }
        if self.collection:
            try:
                self.collection.upsert(
                    ids=[doc_id],
                    documents=[document_text],
                    metadatas=[metadata]
                )
            except Exception:
                pass
        self.fallback_docs[doc_id] = {"text": document_text, "metadata": metadata}

    def retrieve_patient_context(
        self,
        patient_id: int,
        query: str,
        n_results: int = 3
    ) -> List[str]:
        """Retrieves patient-specific reports and relevant guidelines."""
        results = []
        if self.collection:
            try:
                # Query patient reports
                patient_results = self.collection.query(
                    query_texts=[query],
                    n_results=n_results,
                    where={"patient_id": str(patient_id)}
                )
                if patient_results and "documents" in patient_results:
                    for doc_list in patient_results["documents"]:
                        results.extend(doc_list)

                # Also query general medical knowledge
                guideline_results = self.collection.query(
                    query_texts=[query],
                    n_results=2,
                    where={"type": "clinical_guideline"}
                )
                if guideline_results and "documents" in guideline_results:
                    for doc_list in guideline_results["documents"]:
                        results.extend(doc_list)
                return results
            except Exception:
                pass

        # Fallback keyword match in memory
        for doc in self.fallback_docs.values():
            meta = doc["metadata"]
            if meta.get("patient_id") == str(patient_id) or meta.get("type") == "clinical_guideline":
                results.append(doc["text"])

        return results[:n_results + 2]

    def delete_patient_records(self, patient_id: int) -> None:
        """Removes all embeddings associated with a patient upon account deletion."""
        if self.collection:
            try:
                self.collection.delete(where={"patient_id": str(patient_id)})
            except Exception:
                pass
        to_del = [k for k, v in self.fallback_docs.items() if v["metadata"].get("patient_id") == str(patient_id)]
        for k in to_del:
            self.fallback_docs.pop(k, None)



vector_store = MedicalVectorStore()
