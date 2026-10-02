#!/usr/bin/env bash
set +e
exec > >(tee /tmp/probe.txt) 2>&1
SRC=$(find .gradle/mavenizer/repo -name '*-sources.jar' -size +1M | head -1)
mkdir -p /tmp/src && unzip -qo "$SRC" -d /tmp/src && cd /tmp/src
F=net/minecraft/world/level/levelgen/structure/StructurePiece.java
grep -n "protected StructurePiece(\|public abstract\|protected abstract\|public void postProcess\|abstract void postProcess\|placeBlock(\|getBlock(\|createChest\|protected final void addAdditionalSaveData\|getBoundingBox()" $F | head -30
cat net/minecraft/world/level/levelgen/structure/pieces/StructurePieceType.java | head -60 | grep -n "interface\|Context\|load\|StructurePieceType "
F=$(find . -name EndCityStructure.java | head -1); sed -n 1,80p $F | grep -n "findGenerationPoint\|GenerationStub\|generatePieces\|StructurePiecesBuilder\|addPiece"
F=$(find . -name ScatteredFeaturePiece.java | head -1); sed -n 1,80p $F
grep -n "STRUCTURE_PIECE" net/minecraft/core/registries/Registries.java
F=$(find . -name StructurePieceSerializationContext.java | head -1); grep -n "record\|public" $F | head -5
exit 0
