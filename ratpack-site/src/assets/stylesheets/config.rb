require 'java'

asset_pipe_path = Java::AssetPipeline::AssetPipelineConfigHolder.getResolvers()[0].getBaseDirectory().getCanonicalPath()
images_path = "#{asset_pipe_path}/images"
fonts_path = "#{asset_pipe_path}/stylesheets/fonts"

relative_assets = false
# http_path = "/"
http_images_path = "./"
http_fonts_path = "./fonts"
# add_import_path = "../ratpack-stylesheets"
