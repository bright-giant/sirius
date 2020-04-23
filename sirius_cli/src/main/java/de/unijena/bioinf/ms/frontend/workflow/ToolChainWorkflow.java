package de.unijena.bioinf.ms.frontend.workflow;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ms.frontend.subtools.DataSetJob;
import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.PostprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.config.AddConfigsJob;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ToolChainWorkflow implements Workflow {
    protected final static Logger LOG = LoggerFactory.getLogger(ToolChainWorkflow.class);
    protected final ParameterConfig parameters;
    private final PreprocessingJob<?> preprocessingJob;
    private final PostprocessingJob<?> postprocessingJob;
    private final InstanceBufferFactory<?> bufferFactory;

    protected List<Object> toolchain;

    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private InstanceBuffer submitter = null;

    public ToolChainWorkflow(@NotNull PreprocessingJob<?> preprocessingJob, @Nullable PostprocessingJob<?> postprocessingJob, @NotNull ParameterConfig parameters, @NotNull List<Object> toolchain, InstanceBufferFactory<?> bufferFactory) {
        this.preprocessingJob = preprocessingJob;
        this.parameters = parameters;
        this.toolchain = toolchain;
        this.postprocessingJob = postprocessingJob;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public void cancel() {
        canceled.set(true);
        if (submitter != null)
            submitter.cancel();
    }

    protected void checkForCancellation() throws InterruptedException {
        if (canceled.get())
            throw new InterruptedException("Workflow was canceled");
    }

    //todo allow dataset jobs da do not have to put all exps into memory
    //todo low io mode: if instance buffer is infinity we do never have to read instances from disk (write only)
    @Override
    public void run() {
        try {
            checkForCancellation();

            //todo the tool chain should not know anything about the project space. that should be outside this class.
            //todo maybe closing the space should be a "postprocess" job??
//            final ProjectSpaceManager project = (ProjectSpaceManager) ;
            // prepare input
            Iterable<? extends Instance> iteratorSource = SiriusJobs.getGlobalJobManager().submitJob(preprocessingJob).awaitResult();
            // build toolchain
            final List<InstanceJob.Factory<?>> instanceJobChain = new ArrayList<>(toolchain.size());
            //job factory for job that add config annotations to an instance
            instanceJobChain.add(new InstanceJob.Factory<>(
                    (jj) -> new AddConfigsJob(parameters),
                    (inst) -> {
                    }
            ));
            // get buffer size
            final int bufferSize = PropertyManager.getInteger("de.unijena.bioinf.sirius.instanceBuffer", "de.unijena.bioinf.sirius.cpu.cores", 0);

            //other jobs
            for (Object o : toolchain) {
                checkForCancellation();
                if (o instanceof InstanceJob.Factory) {
                    instanceJobChain.add((InstanceJob.Factory<?>) o);
                } else if (o instanceof DataSetJob.Factory) {
                    final DataSetJob dataSetJob = ((DataSetJob.Factory<?>) o).makeJob(submitter);
                    submitter = bufferFactory.create(bufferSize, iteratorSource.iterator(), instanceJobChain, dataSetJob);
                    submitter.start();
                    iteratorSource = submitter.submitJob(dataSetJob).awaitResult();

                    checkForCancellation();
                    instanceJobChain.clear();
                } else {
                    throw new IllegalArgumentException("Illegal job Type submitted. Only InstanceJobs and DataSetJobs are allowed");
                }
            }

            // we have no dataset job that ends the chain, so we have to collect resuts from
            // disk to not waste memory -> otherwise the whole buffer thing is useless.
            checkForCancellation();
            if (!instanceJobChain.isEmpty()) {
                submitter = bufferFactory.create(bufferSize, iteratorSource.iterator(), instanceJobChain, null);
                submitter.start();
            }
            LOG.info("Workflow has been finished!");

            checkForCancellation();
            if (postprocessingJob != null){
                LOG.info("Executing Postprocessing...");
                submitter.submitJob(postprocessingJob).awaitResult();
            }

        } catch (ExecutionException e) {
            LOG.error("Error When Executing ToolChain", e);
        } catch (InterruptedException e) {
            LOG.info("Workflow successfully canceled!");
        }
    }
}
