package com.cocos.challenge.instrument;

import java.util.List;

import com.cocos.challenge.api.model.InstrumentDto;
import org.mapstruct.Mapper;

@Mapper
interface InstrumentMapper {

	InstrumentDto toDto(Instrument instrument);

	List<InstrumentDto> toDtos(List<Instrument> instruments);

}
