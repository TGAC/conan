package uk.ac.ebi.fgpt.conan.model;

/**
 * Created by maplesod on 03/02/14.
 */
public interface PipelineStageGroup {

    PipelineStage[] getAllStages();

    PipelineStage[] parseAll(String[] split);

    String allStagesToString();
}
