package com.project.user.service;

import com.project.security.TokenProvider;
import com.project.user.dao.PetMapper;
import com.project.user.dao.UserMapper;
import com.project.user.dto.UserDto;
import com.project.user.dto.UserPetDto;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.project.user.dto.UserDto.fromExistingAndUpdateProfile;

@Service
@Builder
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    //UserMapper 의존성추가
    private final UserMapper userMapper;
    //PetMapper 의존성추가
    private final PetMapper petMapper;
    //FileUploadService 의존성추가
    private final FileUploadService fileUploadService;
    //TokenProvider 의존성추가
    private final TokenProvider tokenProvider;

    //사용자 프로필을 업데이트하는 메서드
    @Override
    public String updateUser(UserDto existingUserDto, MultipartFile userImage) throws IllegalAccessException {
        validateUserNickname(existingUserDto.getUserNickname());
        UserDto updatedUserDto = uploadImageAndUpdateProfile(existingUserDto, userImage);
        updatePetInfo(updatedUserDto);
        userMapper.updateUser(updatedUserDto);
        return generateNewToken(updatedUserDto);
    }

    // 닉네임 중복 검사
    private void validateUserNickname(String userNickname) {
        int count = userMapper.findUserByNickname(userNickname);
        if(count > 0) {
            throw new IllegalArgumentException("Duplicate nickname");
        }
    }

    // 이미지 업로드 및 프로필 경로 업데이트
    private UserDto uploadImageAndUpdateProfile(UserDto existingUserDto, MultipartFile userImage) {
        String imagePath = fileUploadService.uploadFile(userImage);
        return UserDto.fromExistingAndUpdateProfile(existingUserDto, imagePath);
    }

    // 펫 정보 업데이트
    private void updatePetInfo(UserDto userDto) {
        petMapper.deletePetsByUserNum(userDto.getUserNum());
        for(Integer petTypeId : userDto.getPetTypeIds()) {
            UserPetDto userPetDto = UserPetDto.fromUserAndPetTypeId(userDto.getUserNum(), petTypeId);
            petMapper.insertUserPet(userPetDto);
        }
    }

    // 새 토큰 생성
    private String generateNewToken(UserDto updatedUserDto) {
        return tokenProvider.create(updatedUserDto);
    }

    //회원탈퇴 메서드(Update Status)
    @Override
    public void deactivateUser(int userNum) {
        //사용자의 Status를 'INACTIVE'로 변경
        userMapper.updateUserStatus("INACTIVE", userNum);
    }
}
