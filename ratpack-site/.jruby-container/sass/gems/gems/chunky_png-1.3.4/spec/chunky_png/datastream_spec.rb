require 'spec_helper'

describe ChunkyPNG::Datastream do

  describe '.from_io'do
    it "should raise an error when loading a file with a bad signature" do
      filename = resource_file('damaged_signature.png')
      expect { ChunkyPNG::Datastream.from_file(filename) }.to raise_error
    end

    it "should raise an error if the CRC of a chunk is incorrect" do
      filename = resource_file('damaged_chunk.png')
      expect { ChunkyPNG::Datastream.from_file(filename) }.to raise_error
    end
  end

  describe '#metadata' do
    it "should load uncompressed tXTt chunks correctly" do
      filename = resource_file('text_chunk.png')
      ds = ChunkyPNG::Datastream.from_file(filename)
      expect(ds.metadata['Title']).to  eql 'My amazing icon!'
      expect(ds.metadata['Author']).to eql "Willem van Bergen"
    end

    it "should load compressed zTXt chunks correctly" do
      filename = resource_file('ztxt_chunk.png')
      ds = ChunkyPNG::Datastream.from_file(filename)
      expect(ds.metadata['Title']).to eql 'PngSuite'
      expect(ds.metadata['Copyright']).to eql "Copyright Willem van Schaik, Singapore 1995-96"
    end
  end
end
