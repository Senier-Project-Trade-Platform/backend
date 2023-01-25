package com.sptp.backend.art_work.service;

import com.sptp.backend.art_work.event.ArtWorkEvent;
import com.sptp.backend.art_work.repository.ArtWork;
import com.sptp.backend.art_work.repository.ArtWorkRepository;
import com.sptp.backend.art_work.repository.ArtWorkSize;
import com.sptp.backend.art_work.repository.ArtWorkStatus;
import com.sptp.backend.art_work.web.dto.request.ArtWorkSaveRequestDto;
import com.sptp.backend.art_work.web.dto.response.ArtWorkInfoResponseDto;
import com.sptp.backend.art_work.web.dto.response.ArtWorkMyListResponseDto;
import com.sptp.backend.art_work.web.dto.response.BiddingListResponse;
import com.sptp.backend.art_work_image.repository.ArtWorkImage;
import com.sptp.backend.art_work_image.repository.ArtWorkImageRepository;
import com.sptp.backend.art_work_keyword.repository.ArtWorkKeyword;
import com.sptp.backend.art_work_keyword.repository.ArtWorkKeywordRepository;
import com.sptp.backend.auction.repository.Auction;
import com.sptp.backend.auction.repository.AuctionRepository;
import com.sptp.backend.auction.repository.AuctionStatus;
import com.sptp.backend.aws.service.AwsService;
import com.sptp.backend.aws.service.FileService;
import com.sptp.backend.bidding.repository.Bidding;
import com.sptp.backend.bidding.repository.BiddingRepository;
import com.sptp.backend.common.KeywordMap;
import com.sptp.backend.common.NotificationCode;
import com.sptp.backend.common.entity.BaseEntity;
import com.sptp.backend.common.exception.CustomException;
import com.sptp.backend.common.exception.ErrorCode;
import com.sptp.backend.member.repository.Member;
import com.sptp.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtWorkService extends BaseEntity {

    private final ArtWorkRepository artWorkRepository;
    private final ArtWorkKeywordRepository artWorkKeywordRepository;
    private final ArtWorkImageRepository artWorkImageRepository;
    private final MemberRepository memberRepository;
    private final AwsService awsService;
    private final FileService fileService;

    private final BiddingRepository biddingRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final AuctionRepository auctionRepository;

    @Value("${aws.storage.url}")
    private String storageUrl;

    @Transactional
    public Long saveArtWork(Long loginMemberId, ArtWorkSaveRequestDto dto) throws IOException {

        List<Auction> latestScheduledAuction = auctionRepository.findLatestScheduledAuction();
        if (latestScheduledAuction.size() == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND_AUCTION_SCHEDULED);
        }

        Member findMember = getMemberOrThrow(loginMemberId);

        checkExistsImage(dto);

        String GuaranteeImageUUID = UUID.randomUUID().toString();
        String GuaranteeImageEXT = fileService.extractExt(dto.getGuaranteeImage().getOriginalFilename());

        String mainImageUUID = UUID.randomUUID().toString();
        String mainImageEXT = fileService.extractExt(dto.getImage()[0].getOriginalFilename());

        ArtWork artWork = ArtWork.builder()
                .member(findMember)
                .title(dto.getTitle())
                .material(dto.getMaterial())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .statusDescription(dto.getStatusDescription())
                .guaranteeImage(GuaranteeImageUUID + "." + GuaranteeImageEXT)
                .mainImage(mainImageUUID + "." + mainImageEXT)
                .genre(dto.getGenre())
                .artWorkSize(ArtWorkSize.builder().size(dto.getSize()).length(dto.getLength()).width(dto.getWidth()).height(dto.getHeight()).build())
                .frame(dto.isFrame())
                .description(dto.getDescription())
                .productionYear(dto.getProductionYear())
                .auction(latestScheduledAuction.get(0))
                .saleStatus(ArtWorkStatus.REGISTERED.getType())
                .build();

        ArtWork savedArtWork = artWorkRepository.save(artWork);
        awsService.uploadImage(dto.getGuaranteeImage(), GuaranteeImageUUID);
        awsService.uploadImage(dto.getImage()[0], mainImageUUID);
        saveArtImages(dto.getImage(), artWork);
        saveArtKeywords(dto.getKeywords(), artWork);

        eventPublisher.publishEvent(new ArtWorkEvent(findMember, artWork, NotificationCode.SAVE_ARTWORK));

        return savedArtWork.getId();
    }

    public void saveArtImages(MultipartFile[] files, ArtWork artWork) throws IOException {

        for (MultipartFile file : files) {

            String imageUUID = UUID.randomUUID().toString();
            String imageEXT = fileService.extractExt(file.getOriginalFilename());

            ArtWorkImage artWorkImage = ArtWorkImage.builder()
                    .artWork(artWork)
                    .image(imageUUID + "." + imageEXT)
                    .build();

            artWorkImageRepository.save(artWorkImage);
            awsService.uploadImage(file, imageUUID);
        }
    }

    public void saveArtKeywords(String[] keywords, ArtWork artWork) {

        for (String keyword : keywords) {

            KeywordMap.checkExistsKeyword(keyword);

            ArtWorkKeyword artWorkKeyword = ArtWorkKeyword.builder()
                    .artWork(artWork)
                    .keywordId(KeywordMap.map.get(keyword))
                    .build();

            artWorkKeywordRepository.save(artWorkKeyword);
        }
    }

    public void checkExistsImage(ArtWorkSaveRequestDto dto) {
        if (dto.getGuaranteeImage().isEmpty() || dto.getImage()[0].isEmpty()) {
            throw new CustomException(ErrorCode.SHOULD_EXIST_IMAGE);
        }
    }

    public void bid(Long loginMemberId, Long artWorkId, Long price) {

        ArtWork artWork = getArtWorkOrThrow(artWorkId);
        Member member = getMemberOrThrow(loginMemberId);

        Long topPrice = getTopPrice(artWork);
        Bidding bidding = biddingRepository.findByArtWorkAndMember(artWork, member)
                .orElseGet(() -> saveBidding(artWork, member));

        bidding.raisePrice(topPrice, price);
    }

    private ArtWork getArtWorkOrThrow(Long artWorkId) {
        return artWorkRepository.findById(artWorkId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ARTWORK));
    }

    private Member getMemberOrThrow(Long loginMemberId) {
        return memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_MEMBER));
    }

    private Long getTopPrice(ArtWork artWork) {

        Optional<Bidding> topPriceBiddingOptional = biddingRepository.getFirstByArtWorkOrderByPriceDesc(artWork);

        if (topPriceBiddingOptional.isEmpty()) {
            return Long.valueOf(artWork.getPrice());
        }

        return topPriceBiddingOptional.get().getPrice();
    }

    private Bidding saveBidding(ArtWork artWork, Member member) {
        Bidding bidding = biddingRepository.save(Bidding.builder()
                .artWork(artWork)
                .member(member)
                .build());

        bidding.validateAuctionPeriod();

        return bidding;
    }

    @Transactional(readOnly = true)
    public ArtWorkInfoResponseDto getArtWork(Long artWorkId) {

        ArtWork findArtWork = artWorkRepository.findById(artWorkId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ARTWORK));

        Member findArtist = memberRepository.findById(findArtWork.getMember().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ARTIST));

        List<ArtWorkImage> artWorkImages = artWorkImageRepository.findByArtWorkId(artWorkId);
        List<ArtWorkKeyword> artWorkKeywords = artWorkKeywordRepository.findByArtWorkId(artWorkId);

        return ArtWorkInfoResponseDto.builder()
                .artist(ArtWorkInfoResponseDto.ArtistDto.from(findArtist, storageUrl))
                .artWork(ArtWorkInfoResponseDto.ArtWorkDto.from(findArtWork, artWorkImages, artWorkKeywords, storageUrl))
                .build();
    }

    @Transactional(readOnly = true)
    public List<ArtWorkMyListResponseDto> getMyArtWorkList(Long loginMemberId) {

        Member findMember = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_MEMBER));

        List<ArtWork> findArtWorkList = artWorkRepository.findByMemberId(findMember.getId());

        List<ArtWorkMyListResponseDto> artWorkMyListResponseDtoList = new ArrayList<>();

        for (ArtWork artWork : findArtWorkList) {
            ArtWorkMyListResponseDto artWorkMyListResponseDto = classifyArtWork(artWork);
            artWorkMyListResponseDtoList.add(artWorkMyListResponseDto);
        }

        return artWorkMyListResponseDtoList;
    }

    private ArtWorkMyListResponseDto classifyArtWork(ArtWork artWork) {

        Long topPrice = null;

        if (!artWork.getSaleStatus().equals(ArtWorkStatus.REGISTERED.getType())) {

            if (biddingRepository.existsByArtWorkId(artWork.getId())) {
                topPrice = getTopPrice(artWork);
            }
        }

        return ArtWorkMyListResponseDto.builder()
                .id(artWork.getId())
                .title(artWork.getTitle())
                .turn(artWork.getAuction().getTurn())
                .image(storageUrl + artWork.getMainImage())
                .artistName(artWork.getMember().getNickname())
                .auctionStatus(artWork.getSaleStatus())
                .biddingStatus(topPrice)
                .build();
    }

    @Transactional(readOnly = true)
    public BiddingListResponse getBiddingList(Long artWorkId) {

        ArtWork artWork = getArtWorkOrThrow(artWorkId);
        List<Bidding> biddingList = biddingRepository.findAllByArtWorkOrderByPriceDesc(artWork);

        return BiddingListResponse.builder()
                .artWork(BiddingListResponse.ArtWorkDto.of(artWork, getTopPrice(artWork)))
                .auction(BiddingListResponse.AuctionDto.from(artWork.getAuction()))
                .biddingList(biddingList.stream().map(BiddingListResponse.BiddingDto::from)
                        .collect(Collectors.toList()))
                .totalBiddingCount(biddingList.size())
                .build();
    }

//    private ArtWorkMyListResponseDto classifyArtWork(ArtWork artWork) {
//
//        Long topPrice = 0L;
//
//        if (artWork.getSaleStatus().equals(ArtWorkStatus.PROCESSING.getType())) {
//
//        }
//    }
}
