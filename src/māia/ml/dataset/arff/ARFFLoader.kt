package māia.ml.dataset.arff

import māia.configure.Configurable
import māia.configure.Configuration
import māia.configure.ConfigurationElement
import māia.configure.ConfigurationItem
import māia.ml.dataset.DataBatch
import māia.ml.dataset.primitive.PrimitiveDataBatch

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
class ARFFLoader : Configurable<ARFFLoaderConfiguration> {

    @Configurable.Register<ARFFLoader, ARFFLoaderConfiguration>(
        ARFFLoader::class,
        ARFFLoaderConfiguration::class
    )
    constructor(block : ARFFLoaderConfiguration.() -> Unit = {}) : super(block)

    constructor(configuration: ARFFLoaderConfiguration) : super(configuration)

    fun load() : DataBatch<*, *> {
        return load(configuration.filename)
    }

}

class ARFFLoaderConfiguration : Configuration() {

    @ConfigurationElement.WithMetadata("The name of the ARFF file to load")
    var filename by ConfigurationItem { "" }

}
