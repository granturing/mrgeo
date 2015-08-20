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

package org.mrgeo.opimage;

import org.mrgeo.data.DataProviderFactory;
import org.mrgeo.data.DataProviderFactory.AccessMode;
import org.mrgeo.data.ProviderProperties;
import org.mrgeo.data.image.MrsImageDataProvider;
import org.mrgeo.image.MrsImagePyramidMetadata;
import org.mrgeo.mapreduce.formats.TileClusterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.IOException;


public class MrsPyramidDescriptor extends OperationDescriptorImpl implements RenderedImageFactory
{
private static final Logger log = LoggerFactory.getLogger(MrsPyramidDescriptor.class);

  private static final long serialVersionUID = 1L;

  public MrsPyramidDescriptor()
  {
    // I realize this formatting is horrendous, but Java won't let me assign
    // variables before calling super.
    super(new String[][] 
        { 
          { "GlobalName", MrsPyramidDescriptor.class.getName() }, 
          { "LocalName", MrsPyramidDescriptor.class.getName() },
          { "Vendor", "com.spadac" }, { "Description", "Allows OpChains to access MrsImage tiles." },
          { "DocURL", "http://www.spadac.com/" }, { "Version", "1.0" } 
        },
        new String[] { RenderedRegistryMode.MODE_NAME },
        0,
        new String[] { "path", "level", "tileClusterInfo", "providerProperties" },
        new Class[] { String.class , Long.class, TileClusterInfo.class, ProviderProperties.class },
        new Object[] { NO_PARAMETER_DEFAULT, (long) -1,
                      NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT },
        null);
  }

  public static RenderedImage create(MrsImageDataProvider dp)
  {
    // Use default tile cluster info of 1 tile
    TileClusterInfo tileClusterInfo = new TileClusterInfo();
    return create(dp, tileClusterInfo);
  }

  public static RenderedImage create(MrsImageDataProvider dp,
      TileClusterInfo tileClusterInfo)
  {
    try
    {
      MrsImagePyramidMetadata metadata = dp.getMetadataReader().read();
      return create(dp, metadata.getMaxZoomLevel(), tileClusterInfo,
          dp.getProviderProperties());
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    
    return null;
  }

  public static RenderedImage create(MrsImageDataProvider dp,
      int level, TileClusterInfo tileClusterInfo, final ProviderProperties providerProperties)
  {
    ParameterBlock paramBlock = new ParameterBlock();
    paramBlock.add(dp.getResourceName());
    paramBlock.add(new Long(level));
    paramBlock.add(tileClusterInfo);
    if (providerProperties == null)
    {
      paramBlock.add(new ProviderProperties());
    }
    else
    {
      paramBlock.add(providerProperties);
    }


    return JAI.create(MrsPyramidDescriptor.class.getName(), paramBlock, null);
  }
  

  @Override
  public RenderedImage create(ParameterBlock params, RenderingHints hints)
  {
    try
    {
      // TODO: Based on parameters passed in, we need to reinstantiate the
      // MrsImageDataProvider here so we can pass it to the create method.
      // Param 0 will be the resource name.
      // Another param should contain the property settings required for the
      // specific data provider. Normally this is a Configuration object.

      Long level = (Long)params.getObjectParameter(1);
      TileClusterInfo tileClusterInfo = (TileClusterInfo)params.getObjectParameter(2);
      ProviderProperties providerProperties = (ProviderProperties)params.getObjectParameter(3);

      log.info("Creating MrsPyramidOpImage");
      log.info("  name: {}", params.getObjectParameter(0));
      log.info("  zoom: {}", level);
      log.info("  tileClusterInfo: ");
      log.info("    width: {}", tileClusterInfo.getWidth());
      log.info("    height: {}", tileClusterInfo.getHeight());
      log.info("    offset x: {}", tileClusterInfo.getOffsetX());
      log.info("    offset y: {}", tileClusterInfo.getOffsetY());
      log.info("    stride: {}", tileClusterInfo.getStride());
      log.info("  provider properties: {}", providerProperties.toDelimitedString());

      log.info("  ParameterBlock");
      Class[] classes = params.getParamClasses();

      for(int i = 0; i < params.getNumParameters(); i++)
      {
        log.info("    {} {} {}", i, classes[i], params.getObjectParameter(i));
      }
      MrsImageDataProvider dp = DataProviderFactory.getMrsImageDataProvider(
          (String) params.getObjectParameter(0),
          AccessMode.READ, providerProperties);


      log.info("Creating MrsPyramidOpImage");
      log.info("  name: {}", dp.getResourceName());
      log.info("  zoom: {}", level);
      log.info("  tileClusterInfo: ");
      log.info("    width: {}", tileClusterInfo.getWidth());
      log.info("    height: {}", tileClusterInfo.getHeight());
      log.info("    offset x: {}", tileClusterInfo.getOffsetX());
      log.info("    offset y: {}", tileClusterInfo.getOffsetY());
      log.info("    stride: {}", tileClusterInfo.getStride());
      log.info("  provider properties: {}", providerProperties.toDelimitedString());

      return MrsPyramidOpImage.create(dp, level, tileClusterInfo);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      throw new MrsPyramidOpImage.MrsImageOpImageException(e);
    }
  }

}
