/*
 * Copyright 2009-2015 DigitalGlobe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.mrgeo.ingest

import java.io.Externalizable

import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.PairRDDFunctions
import org.mrgeo.data.DataProviderFactory
import org.mrgeo.data.DataProviderFactory.AccessMode
import org.mrgeo.data.raster.{RasterUtils, RasterWritable}
import org.mrgeo.data.tile.TileIdWritable
import org.mrgeo.utils.{HadoopUtils, SparkUtils}

class IngestLocal extends IngestImage with Externalizable {

  override def execute(context: SparkContext): Boolean = {

    val input = inputs(0) // there is only 1 input here...

    val format = new SequenceFileInputFormat[TileIdWritable, RasterWritable]

    val job: Job = Job.getInstance(HadoopUtils.createConfiguration())


    val rawtiles = context.sequenceFile(input, classOf[TileIdWritable], classOf[RasterWritable])

    // this is stupid, but because the way hadoop input formats may reuse the key/value objects,
    // if we don't do this, all the data will eventually collapse into a single entry.
    val mapped = rawtiles.map(tile => {
      (new TileIdWritable(tile._1), RasterWritable.toWritable(RasterWritable.toRaster(tile._2)))
    })

    val mergedTiles = new PairRDDFunctions(mapped).reduceByKey((r1, r2) => {
      val src = RasterWritable.toRaster(r1)
      val dst = RasterUtils.makeRasterWritable(RasterWritable.toRaster(r2))

      RasterUtils.mosaicTile(src, dst, nodata)
      RasterWritable.toWritable(dst)

    })


    val dp = DataProviderFactory.getMrsImageDataProvider(output, AccessMode.OVERWRITE, providerproperties)

    val raster = RasterWritable.toRaster(mergedTiles.first()._2)
    SparkUtils.saveMrsPyramidRDD(mergedTiles, dp, zoom, tilesize, nodata, context.hadoopConfiguration,
      bounds = this.bounds, bands = this.bands, tiletype = this.tiletype,
      protectionlevel = this.protectionlevel, providerproperties = this.providerproperties)

    true
  }

}