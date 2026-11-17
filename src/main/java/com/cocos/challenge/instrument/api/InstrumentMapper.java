package com.cocos.challenge.instrument.api;

import java.util.List;

import com.cocos.challenge.instrument.domain.Instrument;
import com.cocos.challenge.openapi.model.InstrumentDto;
import org.mapstruct.Mapper;

@Mapper
interface InstrumentMapper {

	InstrumentDto toDto(Instrument instrument);

	List<InstrumentDto> toDtos(List<Instrument> instruments);

}
