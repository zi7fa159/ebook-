export enum PixelFormat {
  Mono = 'Mono',
  RGB = 'RGB',
  BayerRGGB = 'RGGB',
  BayerBGGR = 'BGGR',
  BayerGRBG = 'GRBG',
  BayerGBRG = 'GBRG',
}

export enum Endianness {
  Little = 'Little',
  Big = 'Big',
}

export interface ImageMetadata {
  width: number;
  height: number;
  bitDepth: number;
  pixelFormat: PixelFormat;
  endianness: Endianness;
  timestamp?: number;
}

export interface Frame {
  data: Uint8Array | Uint16Array | Float32Array;
  metadata: ImageMetadata;
}

export interface ProjectSession {
  id: string;
  name: string;
  frames: Frame[];
  qualityScores: number[];
  selectedFrameIndices: number[];
  referenceFrameIndex?: number;
}

export interface Point {
  x: number;
  y: number;
}

export interface AlignmentPoint extends Point {
  size: number;
  weight: number;
  searchRadius: number;
}
