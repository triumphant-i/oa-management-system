<template>
  <div class="oa-meeting" ref="scrollContainer">
    <div class="banner">
      <div class="banner-text">
        <h2 class="banner-title">会议室预订</h2>
        <p class="banner-desc">高效协作 · 空间共享</p>
      </div>
      <div class="banner-icon">
        <van-icon name="location-o" size="48" color="#6c5ce7" />
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f7ff);">
        <p class="stat-label">总会议室</p>
        <span class="stat-num" style="color: #3677ef;">{{ roomList.length }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff4);">
        <p class="stat-label">空闲中</p>
        <span class="stat-num" style="color: #00b894;">{{ stats.free }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fff8ef);">
        <p class="stat-label">使用中</p>
        <span class="stat-num" style="color: #ff6b35;">{{ stats.busy }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #fef0f0);" @click="showMyBookings = true">
        <p class="stat-label">我的预订</p>
        <span class="stat-num" style="color: #e17055;">{{ activeBookings.length }}</span>
      </div>
      <div class="stat-card" style="background: linear-gradient(135deg, #fff, #f0fff0);" @click="openMyInvited">
        <p class="stat-label">我的会议</p>
        <span class="stat-num" style="color: #00b894;">{{ invitedActiveCount }}</span>
      </div>
      <div v-if="isAdmin" class="stat-card" style="background: linear-gradient(135deg, #fff, #f0f0ff);" @click="openAdminPanel">
        <p class="stat-label">全部会议</p>
        <span class="stat-num" style="color: #6c5ce7;">📋</span>
      </div>
      <div v-if="isAdmin" class="stat-card" style="background: linear-gradient(135deg, #fff, #fff0f5);" @click="openRoomManage">
        <p class="stat-label">会议室管理</p>
        <span class="stat-num" style="color: #e84393;">⚙️</span>
      </div>
    </div>

    <!-- 日期选择 -->
    <div class="date-wrap">
      <van-icon name="calendar-o" size="20" color="#6c5ce7" />
      <span class="date-text">{{ currentDate }}</span>
      <van-icon name="arrow-down" size="16" color="#6c5ce7" @click="showCalendar = true" />
    </div>

    <!-- 搜索栏 -->
    <div class="search-wrap">
      <van-search
        v-model="roomSearchKey"
        placeholder="搜索会议室名称/楼层"
        shape="round"
        style="padding: 0 0 12px;"
      />
    </div>

    <!-- 加载状态 -->
    <div class="loading-wrap" v-if="loading">
      <van-loading size="40" text="加载中..." vertical />
    </div>

    <!-- 会议室列表 -->
    <div class="room-list" v-else>
      <div class="room-item" v-for="item in filteredRoomList" :key="item.id" :id="'room-' + item.id">
        <div class="room-header" @click="goDetail(item.id)">
          <div class="room-info">
            <h3 class="room-name">{{ item.name }}</h3>
            <span class="room-status" :class="item.status === '可用' ? 'status-free' : 'status-busy'">
              {{ item.status === '可用' ? '空闲' : '使用中' }}
            </span>
          </div>
          <span class="room-capacity"><van-icon name="user-o" size="14" /> {{ item.capacity }}人</span>
        </div>
        <div class="room-body" @click="goDetail(item.id)">
          <span class="room-location">{{ item.location || item.floor || '未知位置' }}</span>
          <div class="room-equipment">
            <span class="equip-tag" v-for="equip in getEquipmentList(item.equipment)" :key="equip">{{ equip }}</span>
          </div>
        </div>
        <div class="room-footer">
          <!-- 连续时间轴甘特图（分三行：上午/下午/晚上） -->
          <div class="timeline-container">
            <div class="timeline-row" v-for="(period, pIdx) in timePeriods" :key="pIdx">
              <div class="period-label">{{ period.label }}</div>
              <div class="period-content">
                <!-- 刻度 -->
                <div class="timeline-ruler">
                  <span class="ruler-mark" v-for="h in period.marks" :key="h" :style="{ left: getMarkPercent(h, period) + '%' }">
                    {{ String(h).padStart(2, '0') }}:00
                  </span>
                </div>
                <!-- 时间轴 -->
                <div class="timeline-track" 
                     :ref="el => setTimelineRef(item.id, pIdx, el)"
                     data-period-index="pIdx"
                     @mousedown="onDragStartPeriod($event, item, pIdx)"
                     @mousemove="onDragMovePeriod($event, item, pIdx)"
                     @mouseup="onDragEndPeriod($event, item)"
                     @mouseleave="onDragEndPeriod($event, item)"
                     @touchstart="onDragStartPeriod($event, item, pIdx)"
                     @touchmove="onDragMovePeriod($event, item, pIdx)"
                     @touchend="onDragEndPeriod($event, item)"
                >
                  <div 
                    v-for="booking in getPeriodBookings(item.id, pIdx)" 
                    :key="booking.id"
                    class="booked-block"
                    :style="getPeriodBlockStyle(booking, pIdx)"
                    @click.stop="showBookingInfo(booking)"
                  >
                    <span class="block-label">{{ booking.title }}</span>
                  </div>
                  <div 
                    v-if="dragState.active && dragState.roomId === item.id && dragState.periodIdx === pIdx"
                    class="drag-block"
                    :style="getPeriodDragStyle(pIdx)"
                  >
                    <span class="drag-label">{{ dragState.startStr }} - {{ dragState.endStr }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="book-btn-container">
            <van-button 
              type="primary" 
              size="small" 
              class="book-btn" 
              @click="goReserve(item)"
              :disabled="roomFullMap[item.id] || item.status !== '可用'"
            >
              {{ roomFullMap[item.id] ? '已约满' : '预订' }}
            </van-button>
          </div>
        </div>
      </div>
      <div class="empty-state" v-if="filteredRoomList.length === 0">
        <van-icon name="location-o" size="48" color="#ccc" />
        <p>暂无会议室</p>
      </div>
    </div>

    <!-- ===== 底部返回键 ===== -->
    <div class="bottom-bar">
      <van-button plain block size="large" @click="$router.back()" class="back-btn">
        <van-icon name="arrow-left" size="18" /> 返回
      </van-button>
    </div>

    <div class="safe-bottom"></div>

    <!-- ============================================= -->
    <!-- ===== 一键到顶 / 一键到底 浮动按钮 ===== -->
    <!-- ============================================= -->
    <div class="scroll-buttons" v-show="showScrollButtons">
      <div class="scroll-btn" @click="scrollToTop" v-show="showTopBtn">
        <van-icon name="back-top" size="22" color="#6c5ce7" />
        <span>顶部</span>
      </div>
      <div class="scroll-btn" @click="scrollToBottom" v-show="showBottomBtn">
        <van-icon name="down" size="22" color="#6c5ce7" />
        <span>底部</span>
      </div>
    </div>

    <!-- ============================================= -->
    <!-- ===== 日历选择 ===== -->
    <!-- ============================================= -->
    <van-calendar 
      v-model="calendarValue"
      type="date" 
      :show="showCalendar"
      @confirm="onConfirmDate" 
      @close="showCalendar = false"
      :min-date="minDate"
      title="选择日期"
    />

    

    <!-- ============================================= -->
    <!-- ===== 我的预订列表 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showMyBookings" position="bottom" round style="padding:20px 16px 30px;max-height:75vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">📋 我的预订</h3>
        <van-icon name="close" size="22" @click="showMyBookings = false" />
      </div>

      <!-- 搜索栏 -->
      <div class="booking-search">
        <van-search
          v-model="bookingSearchKey"
          placeholder="搜索会议名称"
          shape="round"
          style="padding:0;"
        />
        <div class="search-date-btn" @click="showBookingDatePicker = true">
          <van-icon name="calendar-o" size="16" />
          <span>{{ bookingSearchDate || '全部日期' }}</span>
        </div>
      </div>

      <!-- 标签页 -->
      <van-tabs v-model:active="bookingTab" shrink>
        <van-tab title="进行中" :badge="activeBookings.length > 0 ? activeBookings.length : ''">
          <div class="booking-list">
            <div class="booking-item" v-for="item in activeBookings" :key="item.id">
              <div class="booking-header">
                <span class="booking-room">{{ item.roomName || item.roomId }}</span>
                <span class="booking-status" :class="item.status === '进行中' ? 'active-status' : 'confirmed'">
                  {{ item.status }}
                </span>
              </div>
              <div class="booking-body">
                <span class="booking-title">{{ item.title }}</span>
                <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
              </div>
              <div class="booking-footer">
                <span class="booking-participants">👥 {{ item.participants || '无' }}</span>
                <div class="booking-actions">
                  <van-button v-if="item.status === '已预约' || item.status === '进行中'" size="mini" type="primary" @click="goControl(item)">进入会议</van-button>
                  <van-button size="mini" plain type="primary" @click="editBooking(item)">修改</van-button>
                  <van-button size="mini" plain type="danger" @click="cancelBooking(item.id)">取消</van-button>
                </div>
              </div>
            </div>
            <div class="empty-state" v-if="activeBookings.length === 0">
              <van-icon name="calendar-o" size="48" color="#ccc" />
              <p>暂无进行中的会议</p>
            </div>
          </div>
        </van-tab>
        <van-tab title="已结束" :badge="historyBookings.length > 0 ? historyBookings.length : ''">
          <div class="booking-list">
            <div class="booking-item history-item" v-for="item in historyBookings" :key="item.id">
              <div class="booking-header">
                <span class="booking-room">{{ item.roomName || item.roomId }}</span>
                <span class="booking-status ended-status">已结束</span>
              </div>
              <div class="booking-body">
                <span class="booking-title">{{ item.title }}</span>
                <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
              </div>
              <div class="booking-footer">
                <span class="booking-participants">👥 {{ item.participants || '无' }}</span>
              </div>
            </div>
            <div class="empty-state" v-if="historyBookings.length === 0">
              <van-icon name="clock-o" size="48" color="#ccc" />
              <p>暂无历史会议</p>
            </div>
          </div>
        </van-tab>
      </van-tabs>
    </van-popup>

    <!-- 预订搜索日期选择 -->
    <van-calendar
      v-model="bookingCalendarValue"
      type="date"
      :show="showBookingDatePicker"
      @confirm="onConfirmBookingDate"
      @close="showBookingDatePicker = false"
      :min-date="minDate"
      title="选择日期筛选"
    />

    <!-- ============================================= -->
    <!-- ===== 我的会议（被邀请参加的） ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showMyInvited" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">📅 我的会议</h3>
        <van-icon name="close" size="22" @click="showMyInvited = false" />
      </div>

      <!-- 搜索栏 -->
      <div class="booking-search">
        <van-search
          v-model="invitedSearchKey"
          placeholder="搜索会议名称"
          shape="round"
          style="padding:0;"
        />
        <div class="search-date-btn" @click="showInvitedDatePicker = true">
          <van-icon name="calendar-o" size="16" />
          <span>{{ invitedSearchDate || '全部日期' }}</span>
        </div>
      </div>
      <div v-if="invitedSearchDate" style="text-align:center;margin-bottom:8px;">
        <van-tag closeable type="primary" @close="invitedSearchDate = ''">
          日期: {{ invitedSearchDate }}
        </van-tag>
      </div>

      <!-- 标签页 -->
      <van-tabs v-model:active="invitedTab" shrink>
        <van-tab title="待参加" :badge="invitedActiveList.length > 0 ? invitedActiveList.length : ''">
          <div class="booking-list">
            <div class="booking-item invited-item" v-for="item in invitedActiveList" :key="item.id" @click="goMeetingDetail(item)">
              <div class="booking-header">
                <span class="booking-room">{{ item.roomName || ('会议室'+item.roomId) }}</span>
                <span class="booking-status" :class="item.status === '进行中' ? 'active-status' : 'confirmed'">
                  {{ item.status }}
                </span>
              </div>
              <div class="booking-body">
                <span class="booking-title">{{ item.title }}</span>
                <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
                <span class="booking-organizer">组织者：{{ item.organizerName }}</span>
              </div>
            </div>
            <div class="empty-state" v-if="invitedActiveList.length === 0">
              <van-icon name="calendar-o" size="48" color="#ccc" />
              <p>暂无待参加的会议</p>
            </div>
          </div>
        </van-tab>
        <van-tab title="已结束" :badge="invitedHistoryList.length > 0 ? invitedHistoryList.length : ''">
          <div class="booking-list">
            <div class="booking-item history-item" v-for="item in invitedHistoryList" :key="item.id" @click="goMeetingDetail(item)">
              <div class="booking-header">
                <span class="booking-room">{{ item.roomName || ('会议室'+item.roomId) }}</span>
                <span class="booking-status ended-status">{{ item.status === '已取消' ? '已取消' : '已结束' }}</span>
              </div>
              <div class="booking-body">
                <span class="booking-title">{{ item.title }}</span>
                <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
                <span class="booking-organizer">组织者：{{ item.organizerName }}</span>
              </div>
            </div>
            <div class="empty-state" v-if="invitedHistoryList.length === 0">
              <van-icon name="clock-o" size="48" color="#ccc" />
              <p>暂无历史会议</p>
            </div>
          </div>
        </van-tab>
      </van-tabs>
    </van-popup>

    <!-- 我的会议日期选择 -->
    <van-calendar
      v-model="invitedCalendarValue"
      type="date"
      :show="showInvitedDatePicker"
      @confirm="onConfirmInvitedDate"
      @close="showInvitedDatePicker = false"
      :min-date="minDate"
      title="选择日期筛选"
    />

    <!-- 编辑预订弹窗 -->
    <van-popup v-model:show="showEditBooking" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">✏️ 修改预订</h3>
        <van-icon name="close" size="22" @click="showEditBooking = false" />
      </div>

      <van-form @submit="submitEditBooking">
        <van-cell-group inset>
          <van-field
            v-model="editForm.title"
            label="会议主题"
            placeholder="请输入会议主题"
            :rules="[{ required: true, message: '请输入会议主题' }]"
          />
          <van-field
            v-model="editForm.startTime"
            label="开始时间"
            placeholder="请选择"
            is-link
            @click="showEditStartPicker = true"
            :rules="[{ required: true, message: '请选择开始时间' }]"
          />
          <van-field
            v-model="editForm.endTime"
            label="结束时间"
            placeholder="请选择"
            is-link
            @click="showEditEndPicker = true"
            :rules="[
              { required: true, message: '请选择结束时间' },
              { validator: (v) => {
                if (!editForm.startTime || !v) return true
                return v > editForm.startTime
              }, message: '结束时间必须晚于开始时间' }
            ]"
          />
          <van-field
            v-model="editForm.participantsText"
            label="参会人员"
            placeholder="请选择参会人"
            is-link
            readonly
            @click="showEditParticipantPicker = true"
          />
        </van-cell-group>

        <div class="conflict-warning" v-if="editConflictDetected">
          <van-icon name="warning-o" size="16" color="#e17055" />
          <span>该时段已被其他人预订，请选择其他时间</span>
        </div>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showEditBooking = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="editSubmitting" :disabled="editConflictDetected">
            保存修改
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 我的预订参会人选择弹窗 -->
    <van-popup v-model:show="showEditParticipantPicker" position="bottom" round style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">👥 选择参会人</h3>
        <van-button size="small" type="primary" @click="confirmEditParticipants">确定</van-button>
      </div>
      <van-search
        v-model="editParticipantSearchKey"
        placeholder="搜索姓名/部门"
        shape="round"
        style="padding:0 0 10px;"
      />
      <van-checkbox-group v-model="editSelectedParticipantIds">
        <van-cell-group inset>
          <van-cell v-for="emp in filteredEditEmployees" :key="emp.id" :title="emp.name" :label="emp.departmentName || '未分配部门'">
            <template #right-icon>
              <van-checkbox :name="String(emp.id)" shape="square" />
            </template>
          </van-cell>
        </van-cell-group>
      </van-checkbox-group>
      <div class="empty-state" v-if="filteredEditEmployees.length === 0">
        <van-icon name="user-o" size="32" color="#ccc" />
        <p>没有匹配的员工</p>
      </div>
    </van-popup>

    <!-- ============================================= -->
    <!-- ===== 管理员-全部会议管理 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showAdminPanel" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">📋 全部会议管理</h3>
        <van-icon name="close" size="22" @click="showAdminPanel = false" />
      </div>

      <!-- 搜索栏 -->
      <div class="booking-search">
        <van-search
          v-model="adminSearchKey"
          placeholder="搜索会议名称/组织者"
          shape="round"
          style="padding:0;"
        />
        <van-dropdown-menu>
          <van-dropdown-item v-model="adminStatusFilter" :options="adminStatusOptions" />
        </van-dropdown-menu>
      </div>

      <div class="booking-list">
        <div class="booking-item" v-for="item in filteredAdminMeetings" :key="item.id">
          <div class="booking-header">
            <span class="booking-room">{{ item.roomName || ('会议室'+item.roomId) }}</span>
            <span class="booking-status" :class="getAdminStatusClass(item.status)">
              {{ item.status }}
            </span>
          </div>
          <div class="booking-body">
            <span class="booking-title">{{ item.title }}</span>
            <span class="booking-time">{{ formatDate(item.startTime) }} - {{ formatDate(item.endTime) }}</span>
            <span class="booking-organizer">组织者：{{ item.organizerName }}</span>
          </div>
          <div class="booking-footer">
            <span class="booking-participants">👥 {{ item.participants || '无' }}</span>
            <div class="booking-actions" v-if="item.status !== '已结束' && item.status !== '已取消'">
              <van-button size="mini" plain type="primary" @click="adminEditBooking(item)">修改</van-button>
              <van-button size="mini" plain type="danger" @click="adminCancelBooking(item.id)">取消</van-button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-if="filteredAdminMeetings.length === 0">
          <van-icon name="calendar-o" size="48" color="#ccc" />
          <p>暂无会议记录</p>
        </div>
      </div>
    </van-popup>

    <!-- 管理员修改会议弹窗 -->
    <van-popup v-model:show="showAdminEdit" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">✏️ 修改会议（管理员）</h3>
        <van-icon name="close" size="22" @click="showAdminEdit = false" />
      </div>

      <van-form @submit="submitAdminEdit">
        <van-cell-group inset>
          <van-field
            v-model="adminEditForm.title"
            label="会议主题"
            placeholder="请输入会议主题"
            :rules="[{ required: true, message: '请输入会议主题' }]"
          />
          <van-field
            v-model="adminEditForm.dateStr"
            label="会议日期"
            placeholder="请选择日期"
            is-link
            @click="showAdminDatePicker = true"
            readonly
          />
          <van-field
            v-model="adminEditForm.startTime"
            label="开始时间"
            placeholder="请选择"
            is-link
            @click="showAdminStartPicker = true"
            :rules="[{ required: true, message: '请选择开始时间' }]"
          />
          <van-field
            v-model="adminEditForm.endTime"
            label="结束时间"
            placeholder="请选择"
            is-link
            @click="showAdminEndPicker = true"
            :rules="[
              { required: true, message: '请选择结束时间' },
              { validator: (v) => {
                if (!adminEditForm.startTime || !v) return true
                return v > adminEditForm.startTime
              }, message: '结束时间必须晚于开始时间' }
            ]"
          />
          <van-field
            v-model="adminEditForm.participantsText"
            label="参会人员"
            placeholder="请选择参会人"
            is-link
            readonly
            @click="showAdminParticipantPicker = true"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showAdminEdit = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="adminSubmitting">
            保存修改
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 管理员参会人选择弹窗 -->
    <van-popup v-model:show="showAdminParticipantPicker" position="bottom" round style="padding:20px 16px 30px;max-height:70vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">👥 选择参会人</h3>
        <van-button size="small" type="primary" @click="confirmAdminParticipants">确定</van-button>
      </div>
      <van-search
        v-model="adminParticipantSearchKey"
        placeholder="搜索姓名/部门"
        shape="round"
        style="padding:0 0 10px;"
      />
      <van-checkbox-group v-model="adminSelectedParticipantIds">
        <van-cell-group inset>
          <van-cell v-for="emp in filteredAdminEmployees" :key="emp.id" :title="emp.name" :label="emp.departmentName || '未分配部门'">
            <template #right-icon>
              <van-checkbox :name="String(emp.id)" shape="square" />
            </template>
          </van-cell>
        </van-cell-group>
      </van-checkbox-group>
      <div class="empty-state" v-if="filteredAdminEmployees.length === 0">
        <van-icon name="user-o" size="32" color="#ccc" />
        <p>没有匹配的员工</p>
      </div>
    </van-popup>

    <!-- 管理员修改用日期/时间选择器 -->
    <van-calendar
      v-model="adminCalendarValue"
      type="date"
      :show="showAdminDatePicker"
      @confirm="onConfirmAdminDate"
      @close="showAdminDatePicker = false"
      :min-date="minDate"
      title="选择会议日期"
    />
    <van-popup v-model:show="showAdminStartPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmAdminStart" @cancel="showAdminStartPicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showAdminEndPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmAdminEnd" @cancel="showAdminEndPicker = false" title="选择结束时间" />
    </van-popup>

    <!-- ============================================= -->
    <!-- ===== 会议室管理 ===== -->
    <!-- ============================================= -->
    <van-popup v-model:show="showRoomManage" position="bottom" round style="padding:20px 16px 30px;max-height:85vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3 style="margin:0;">⚙️ 会议室管理</h3>
        <van-button size="small" type="primary" @click="openAddRoom">
          <van-icon name="plus" size="14" /> 新增
        </van-button>
        <van-icon name="close" size="22" @click="showRoomManage = false" />
      </div>

      <van-search
        v-model="roomManageSearchKey"
        placeholder="搜索会议室名称/楼层"
        shape="round"
        style="padding:0 0 10px;"
      />

      <div class="room-manage-list">
        <div class="room-manage-item" v-for="item in filteredManageRooms" :key="item.id">
          <div class="room-manage-header">
            <span class="room-manage-name">{{ item.name }}</span>
            <span :class="['room-manage-status', item.status === '可用' ? 'status-free' : 'status-busy']">
              {{ item.status }}
            </span>
          </div>
          <div class="room-manage-info">
            <span>容纳：{{ item.capacity }}人</span>
            <span>位置：{{ item.location || '未设置' }}</span>
          </div>
          <div class="room-manage-equip" v-if="item.equipment">
            设备：{{ item.equipment }}
          </div>
          <div class="room-manage-actions">
            <van-button size="mini" plain type="primary" @click="editRoom(item)">修改</van-button>
            <van-button size="mini" plain type="danger" @click="deleteRoom(item)">删除</van-button>
          </div>
        </div>
        <div class="empty-state" v-if="filteredManageRooms.length === 0">
          <van-icon name="location-o" size="32" color="#ccc" />
          <p>暂无会议室</p>
        </div>
      </div>
    </van-popup>

    <!-- 新增/编辑会议室弹窗 -->
    <van-popup v-model:show="showRoomEdit" position="bottom" round style="padding:20px 16px 30px;max-height:80vh;overflow-y:auto;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <h3 style="margin:0;">{{ roomEditForm.id ? '✏️ 修改会议室' : '➕ 新增会议室' }}</h3>
        <van-icon name="close" size="22" @click="showRoomEdit = false" />
      </div>

      <van-form @submit="submitRoomEdit">
        <van-cell-group inset>
          <van-field
            v-model="roomEditForm.name"
            label="会议室名称"
            placeholder="请输入会议室名称"
            :rules="[{ required: true, message: '请输入会议室名称' }]"
          />
          <van-field
            v-model="roomEditForm.capacity"
            label="容纳人数"
            placeholder="请输入容纳人数"
            type="number"
            :rules="[{ required: true, message: '请输入容纳人数' }]"
          />
          <van-field
            v-model="roomEditForm.location"
            label="位置/楼层"
            placeholder="如：3楼东侧"
          />
          <van-field
            v-model="roomEditForm.equipment"
            label="设备"
            placeholder="如：投影仪,白板,音响（逗号分隔）"
          />
          <van-field
            v-model="roomEditForm.status"
            label="状态"
            is-link
            readonly
            placeholder="请选择状态"
            @click="showRoomStatusPicker = true"
          />
        </van-cell-group>

        <div style="display:flex;gap:12px;margin-top:16px;">
          <van-button plain block @click="showRoomEdit = false">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="roomEditSubmitting">
            {{ roomEditForm.id ? '保存修改' : '新增' }}
          </van-button>
        </div>
      </van-form>
    </van-popup>

    <!-- 会议室状态选择 -->
    <van-popup v-model:show="showRoomStatusPicker" position="bottom" round>
      <van-picker 
        :columns="roomStatusOptions" 
        @confirm="onConfirmRoomStatus" 
        @cancel="showRoomStatusPicker = false" 
        title="选择状态" 
      />
    </van-popup>

    <!-- 编辑时间选择 -->
    <van-popup v-model:show="showEditStartPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEditStart" @cancel="showEditStartPicker = false" title="选择开始时间" />
    </van-popup>
    <van-popup v-model:show="showEditEndPicker" position="bottom" round>
      <van-picker :columns="timeColumns" @confirm="onConfirmEditEnd" @cancel="showEditEndPicker = false" title="选择结束时间" />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { 
  getMeetingRoomList, 
  getMyMeetings, 
  bookMeeting, 
  cancelMeeting, 
  updateMeeting,
  getRoomMeetings,
  checkRoomFull,
  getAllMeetings,
  getMyInvitedMeetings,
  addMeetingRoom,
  updateMeetingRoom,
  deleteMeetingRoom
} from '@/api/meeting'
import { getAllEmployees, getDepartmentList } from '@/api/employee'

const router = useRouter()

// =============================================
// ===== 当前时间 =====
// =============================================
const now = ref(new Date())
let timer = null

// =============================================
// ===== 滚动控制 =====
// =============================================
const scrollContainer = ref(null)
const showScrollButtons = ref(true)
const showTopBtn = ref(false)
const showBottomBtn = ref(true)

// =============================================
// ===== 状态 =====
// =============================================
const loading = ref(true)
  const stats = ref({ free: 0, busy: 0 })
  const currentDate = ref('')
  const showCalendar = ref(false)
  const calendarValue = ref(new Date())
  const roomSearchKey = ref('')

// =============================================
// ===== 会议室数据 =====
// =============================================
const roomList = ref([])
const roomBookings = ref([])
const roomFullMap = ref({})

const filteredRoomList = computed(() => {
  if (!roomSearchKey.value) return roomList.value
  const key = roomSearchKey.value.toLowerCase()
  return roomList.value.filter(room => {
    const nameMatch = room.name && room.name.toLowerCase().includes(key)
    const locationMatch = room.location && room.location.toLowerCase().includes(key)
    return nameMatch || locationMatch
  })
})

// =============================================
// ===== 我的预订 =====
// =============================================
const myBookings = ref([])
const showMyBookings = ref(false)
const bookingSearchKey = ref('')
const bookingSearchDate = ref('')
const showBookingDatePicker = ref(false)
const bookingCalendarValue = ref(new Date())
const bookingTab = ref(0)

const onConfirmBookingDate = (value) => {
  const date = new Date(value)
  bookingSearchDate.value = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showBookingDatePicker.value = false
}

const clearBookingDateFilter = () => {
  bookingSearchDate.value = ''
}

// 过滤后的会议列表
const filteredBookings = computed(() => {
  let result = myBookings.value
  if (bookingSearchKey.value) {
    result = result.filter(b => b.title && b.title.includes(bookingSearchKey.value))
  }
  if (bookingSearchDate.value) {
    result = result.filter(b => {
      const bDate = new Date(b.startTime)
      const bDateStr = `${bDate.getFullYear()}-${String(bDate.getMonth()+1).padStart(2,'0')}-${String(bDate.getDate()).padStart(2,'0')}`
      return bDateStr === bookingSearchDate.value
    })
  }
  return result
})

// 进行中的会议（已预约/进行中/未结束）
const activeBookings = computed(() => {
  return filteredBookings.value.filter(b => b.status !== '已结束' && b.status !== '已取消')
})

// 历史会议（已结束/已取消）
const historyBookings = computed(() => {
  return filteredBookings.value.filter(b => b.status === '已结束' || b.status === '已取消')
})

// =============================================
// ===== 用户信息 =====
// =============================================
const employeeId = ref(1)
const employeeName = ref('用户')
const isAdmin = ref(false)

// =============================================
// ===== 我的会议（被邀请参加的） =====
// =============================================
const showMyInvited = ref(false)
const myInvitedMeetings = ref([])
const invitedSearchKey = ref('')
const invitedSearchDate = ref('')
const showInvitedDatePicker = ref(false)
const invitedCalendarValue = ref(new Date())
const invitedTab = ref(0)

const openMyInvited = async () => {
  showMyInvited.value = true
  await loadMyInvited()
}

const loadMyInvited = async () => {
  try {
    const res = await getMyInvitedMeetings(employeeId.value)
    if (res.code === 0) {
      myInvitedMeetings.value = res.data || []
    }
  } catch (error) {
    console.error('加载我的会议失败:', error)
  }
}

const onConfirmInvitedDate = (value) => {
  const date = new Date(value)
  invitedSearchDate.value = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showInvitedDatePicker.value = false
}

const filteredInvited = computed(() => {
  let result = myInvitedMeetings.value
  if (invitedSearchKey.value) {
    result = result.filter(b => b.title && b.title.includes(invitedSearchKey.value))
  }
  if (invitedSearchDate.value) {
    result = result.filter(b => {
      const bDate = new Date(b.startTime)
      const bDateStr = `${bDate.getFullYear()}-${String(bDate.getMonth()+1).padStart(2,'0')}-${String(bDate.getDate()).padStart(2,'0')}`
      return bDateStr === invitedSearchDate.value
    })
  }
  return result
})

const invitedActiveList = computed(() => {
  return filteredInvited.value.filter(b => b.status !== '已结束' && b.status !== '已取消')
})

const invitedHistoryList = computed(() => {
  return filteredInvited.value.filter(b => b.status === '已结束' || b.status === '已取消')
})

const invitedActiveCount = computed(() => invitedActiveList.value.length)

const goMeetingDetail = (item) => {
  router.push(`/meeting/detail/${item.id}`)
}

// =============================================
// ===== 管理员-全部会议 =====
// =============================================
const showAdminPanel = ref(false)
const adminMeetings = ref([])
const adminSearchKey = ref('')
const adminStatusFilter = ref('')
const adminStatusOptions = [
  { text: '全部状态', value: '' },
  { text: '已预约', value: '已预约' },
  { text: '进行中', value: '进行中' },
  { text: '已结束', value: '已结束' },
  { text: '已取消', value: '已取消' }
]

const filteredAdminMeetings = computed(() => {
  let result = adminMeetings.value
  if (adminSearchKey.value) {
    const key = adminSearchKey.value.toLowerCase()
    result = result.filter(b => 
      (b.title && b.title.toLowerCase().includes(key)) ||
      (b.organizerName && b.organizerName.toLowerCase().includes(key))
    )
  }
  if (adminStatusFilter.value) {
    result = result.filter(b => b.status === adminStatusFilter.value)
  }
  return result
})

const getAdminStatusClass = (status) => {
  if (status === '进行中') return 'active-status'
  if (status === '已预约') return 'confirmed'
  if (status === '已结束') return 'ended-status'
  return 'cancelled'
}

const openAdminPanel = async () => {
  showAdminPanel.value = true
  await loadAllAdminMeetings()
}

const loadAllAdminMeetings = async () => {
  try {
    const res = await getAllMeetings()
    if (res.code === 0) {
      adminMeetings.value = res.data || []
    }
  } catch (error) {
    console.error('加载所有会议失败:', error)
    showToast('加载失败')
  }
}

// 管理员修改
const showAdminEdit = ref(false)
const adminSubmitting = ref(false)
const showAdminDatePicker = ref(false)
const showAdminStartPicker = ref(false)
const showAdminEndPicker = ref(false)
const adminCalendarValue = ref(new Date())
const adminEditForm = ref({
  id: null,
  title: '',
  dateStr: '',
  startTime: '',
  endTime: '',
  participants: '',
  participantIds: '',
  participantsText: ''
})

const showAdminParticipantPicker = ref(false)
const adminParticipantSearchKey = ref('')
const allAdminEmployees = ref([])
const adminSelectedParticipantIds = ref([])
const departmentMap = ref({})

const loadAllEmployees = async () => {
  try {
    const [empRes, deptRes] = await Promise.all([getAllEmployees(), getDepartmentList()])
    if (deptRes.code === 0 && deptRes.data) {
      departmentMap.value = {}
      deptRes.data.forEach(dept => {
        departmentMap.value[dept.id] = dept.name
      })
    }
    if (empRes.code === 0) {
      allAdminEmployees.value = (empRes.data || []).map(emp => ({
        ...emp,
        departmentName: departmentMap.value[emp.departmentId] || '未分配部门'
      }))
    }
  } catch (error) {
    console.error('加载员工列表失败:', error)
  }
}

const filteredAdminEmployees = computed(() => {
  if (!adminParticipantSearchKey.value) return allAdminEmployees.value
  const key = adminParticipantSearchKey.value.toLowerCase()
  return allAdminEmployees.value.filter(emp => {
    const nameMatch = emp.name && emp.name.toLowerCase().includes(key)
    const deptMatch = emp.departmentName && emp.departmentName.toLowerCase().includes(key)
    return nameMatch || deptMatch
  })
})

const confirmAdminParticipants = () => {
  const selectedEmps = allAdminEmployees.value.filter(emp => 
    adminSelectedParticipantIds.value.includes(String(emp.id))
  )
  adminEditForm.value.participantsText = selectedEmps.map(e => e.name).join('，')
  adminEditForm.value.participantIds = adminSelectedParticipantIds.value.join(',')
  adminEditForm.value.participants = String(selectedEmps.length)
  showAdminParticipantPicker.value = false
}

const adminEditBooking = async (item) => {
  adminEditForm.value = {
    id: item.id,
    title: item.title,
    dateStr: new Date(item.startTime).toISOString().split('T')[0],
    startTime: formatTimeOnly(item.startTime),
    endTime: formatTimeOnly(item.endTime),
    participants: String(item.participants || 0),
    participantIds: item.participantIds || '',
    participantsText: item.participants || ''
  }
  
  if (allAdminEmployees.value.length === 0) {
    await loadAllEmployees()
  }
  
  if (item.participantIds) {
    adminSelectedParticipantIds.value = item.participantIds.split(',').filter(id => id)
  } else {
    adminSelectedParticipantIds.value = []
  }
  
  showAdminEdit.value = true
}

const onConfirmAdminDate = (value) => {
  const date = new Date(value)
  adminEditForm.value.dateStr = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showAdminDatePicker.value = false
}

const onConfirmAdminStart = ({ selectedValues }) => {
  adminEditForm.value.startTime = selectedValues[0]
  showAdminStartPicker.value = false
}

const onConfirmAdminEnd = ({ selectedValues }) => {
  adminEditForm.value.endTime = selectedValues[0]
  showAdminEndPicker.value = false
}

const submitAdminEdit = async () => {
  adminSubmitting.value = true
  try {
    const data = {
      id: adminEditForm.value.id,
      title: adminEditForm.value.title,
      startTime: `${adminEditForm.value.dateStr}T${adminEditForm.value.startTime}:00`,
      endTime: `${adminEditForm.value.dateStr}T${adminEditForm.value.endTime}:00`,
      participants: adminEditForm.value.participants,
      participantIds: adminEditForm.value.participantIds
    }
    const res = await updateMeeting(data)
    if (res.code === 0) {
      showToast('修改成功，已通知参会人')
      showAdminEdit.value = false
      await loadAllAdminMeetings()
      await loadMyBookings()
      await loadAllRoomBookings()
    } else {
      showToast(res.msg || '修改失败')
    }
  } catch (error) {
    showToast('修改失败')
  } finally {
    adminSubmitting.value = false
  }
}

const adminCancelBooking = async (id) => {
  showConfirmDialog({
    title: '确认取消',
    message: '确定要取消该会议吗？将通知所有参会人。',
    confirmButtonText: '确定取消',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await cancelMeeting(id)
      if (res.code === 0) {
        showToast('已取消会议')
        await loadAllAdminMeetings()
        await loadMyBookings()
        await loadAllRoomBookings()
      } else {
        showToast(res.msg || '取消失败')
      }
    } catch (error) {
      showToast('取消失败')
    }
  }).catch(() => {})
}

// =============================================
// ===== 会议室管理 =====
// =============================================
const showRoomManage = ref(false)
const showRoomEdit = ref(false)
const roomEditSubmitting = ref(false)
const roomManageSearchKey = ref('')
const showRoomStatusPicker = ref(false)
const roomStatusOptions = [
  { text: '可用', value: '可用' },
  { text: '维修中', value: '维修中' },
  { text: '已停用', value: '已停用' }
]

const roomEditForm = ref({
  id: null,
  name: '',
  capacity: '',
  location: '',
  equipment: '',
  status: '可用'
})

const filteredManageRooms = computed(() => {
  if (!roomManageSearchKey.value) return roomList.value
  const key = roomManageSearchKey.value.toLowerCase()
  return roomList.value.filter(room => {
    const nameMatch = room.name && room.name.toLowerCase().includes(key)
    const locationMatch = room.location && room.location.toLowerCase().includes(key)
    return nameMatch || locationMatch
  })
})

const openRoomManage = async () => {
  showRoomManage.value = true
  await loadRoomList()
}

const openAddRoom = () => {
  roomEditForm.value = {
    id: null,
    name: '',
    capacity: '',
    location: '',
    equipment: '',
    status: '可用'
  }
  showRoomEdit.value = true
}

const editRoom = (item) => {
  roomEditForm.value = {
    id: item.id,
    name: item.name,
    capacity: String(item.capacity || ''),
    location: item.location || '',
    equipment: item.equipment || '',
    status: item.status || '可用'
  }
  showRoomEdit.value = true
}

const onConfirmRoomStatus = ({ selectedValues }) => {
  roomEditForm.value.status = selectedValues[0]
  showRoomStatusPicker.value = false
}

const submitRoomEdit = async () => {
  roomEditSubmitting.value = true
  try {
    const data = {
      name: roomEditForm.value.name,
      capacity: parseInt(roomEditForm.value.capacity),
      location: roomEditForm.value.location,
      equipment: roomEditForm.value.equipment,
      status: roomEditForm.value.status
    }
    
    let res
    if (roomEditForm.value.id) {
      data.id = roomEditForm.value.id
      res = await updateMeetingRoom(data)
    } else {
      res = await addMeetingRoom(data)
    }
    
    if (res.code === 0) {
      showToast(roomEditForm.value.id ? '修改成功' : '新增成功')
      showRoomEdit.value = false
      await loadRoomList()
      updateRoomStatus()
    } else {
      showToast(res.msg || '操作失败')
    }
  } catch (error) {
    showToast('操作失败')
  } finally {
    roomEditSubmitting.value = false
  }
}

const deleteRoom = (item) => {
  showConfirmDialog({
    title: '确认删除',
    message: `确定要删除会议室【${item.name}】吗？`,
    confirmButtonText: '确定删除',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await deleteMeetingRoom(item.id)
      if (res.code === 0) {
        showToast('删除成功')
        await loadRoomList()
        updateRoomStatus()
      } else {
        showToast(res.msg || '删除失败')
      }
    } catch (error) {
      showToast('删除失败')
    }
  }).catch(() => {})
}

// =============================================
// ===== 滚动控制方法 =====
// =============================================
const scrollToTop = () => {
  // 使用平滑滚动到顶部
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
  // 也尝试滚动容器
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: 0,
      behavior: 'smooth'
    })
  }
}

const scrollToBottom = () => {
  // 滚动到底部
  const scrollHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight,
    document.documentElement.offsetHeight,
    document.body.offsetHeight
  )
  window.scrollTo({
    top: scrollHeight,
    behavior: 'smooth'
  })
  if (scrollContainer.value) {
    scrollContainer.value.scrollTo({
      top: scrollContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

// =============================================
// ===== 滚动事件监听 =====
// =============================================
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = Math.max(
    document.documentElement.scrollHeight,
    document.body.scrollHeight
  )
  
  // 显示/隐藏顶部按钮（滚动超过200px显示）
  showTopBtn.value = scrollTop > 200
  
  // 显示/隐藏底部按钮（距离底部超过200px显示）
  const distanceToBottom = documentHeight - (scrollTop + windowHeight)
  showBottomBtn.value = distanceToBottom > 200
}

// =============================================
// ===== 时间轴甘特图（三行：上午/下午/晚上） =====
// =============================================
const timelineRefs = ref({})
const dragState = reactive({
  active: false,
  roomId: null,
  periodIdx: 0,
  startX: 0,
  startPercent: 0,
  endPercent: 0,
  startStr: '',
  endStr: ''
})

// 三个时间段定义
const timePeriods = [
  { label: '上午', startHour: 8, endHour: 12, marks: [8, 9, 10, 11, 12] },
  { label: '下午', startHour: 12, endHour: 18, marks: [12, 13, 14, 15, 16, 17, 18] },
  { label: '晚上', startHour: 18, endHour: 21, marks: [18, 19, 20, 21] }
]

const setTimelineRef = (roomId, periodIdx, el) => {
  const key = `${roomId}_${periodIdx}`
  if (el) {
    timelineRefs.value[key] = el
  }
}

// 获取刻度百分比位置
const getMarkPercent = (hour, period) => {
  const totalMinutes = (period.endHour - period.startHour) * 60
  const minutes = (hour - period.startHour) * 60
  return (minutes / totalMinutes) * 100
}

// 获取时间段内的预约（部分重叠也显示）
const getPeriodBookings = (roomId, periodIdx) => {
  const period = timePeriods[periodIdx]
  const periodStart = new Date(currentDate.value + 'T00:00:00')
  periodStart.setHours(period.startHour, 0, 0, 0)
  const periodEnd = new Date(currentDate.value + 'T00:00:00')
  periodEnd.setHours(period.endHour, 0, 0, 0)
  
  return roomBookings.value
    .filter(b => {
      if (b.roomId !== roomId || b.status === '已取消') return false
      const bStart = new Date(b.startTime)
      const bEnd = new Date(b.endTime)
      return bStart < periodEnd && bEnd > periodStart
    })
    .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
}

// 计算时间段内色块样式
const getPeriodBlockStyle = (booking, periodIdx) => {
  const period = timePeriods[periodIdx]
  const totalMinutes = (period.endHour - period.startHour) * 60
  const periodStart = new Date(currentDate.value + 'T00:00:00')
  periodStart.setHours(period.startHour, 0, 0, 0)
  
  const bStart = new Date(booking.startTime)
  const bEnd = new Date(booking.endTime)
  
  const actualStart = bStart > periodStart ? bStart : periodStart
  const periodEnd = new Date(currentDate.value + 'T00:00:00')
  periodEnd.setHours(period.endHour, 0, 0, 0)
  const actualEnd = bEnd < periodEnd ? bEnd : periodEnd
  
  const startMinutes = (actualStart - periodStart) / (1000 * 60)
  const endMinutes = (actualEnd - periodStart) / (1000 * 60)
  
  const left = Math.max(0, (startMinutes / totalMinutes) * 100)
  const width = Math.max(0, ((endMinutes - startMinutes) / totalMinutes) * 100)
  
  return {
    left: left + '%',
    width: width + '%'
  }
}

// 百分比转时间字符串
const periodPercentToTimeStr = (percent, periodIdx) => {
  const period = timePeriods[periodIdx]
  const totalMinutes = (period.endHour - period.startHour) * 60
  const minutes = (percent / 100) * totalMinutes
  const roundedMinutes = Math.round(minutes / 5) * 5
  const hour = period.startHour + Math.floor(roundedMinutes / 60)
  const minute = roundedMinutes % 60
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

// 获取事件在时间轴上的百分比
const getEventPeriodPercent = (event, roomId, periodIdx) => {
  const key = `${roomId}_${periodIdx}`
  const track = timelineRefs.value[key]
  if (!track) return 0
  const rect = track.getBoundingClientRect()
  const clientX = event.touches ? event.touches[0].clientX : event.clientX
  const x = clientX - rect.left
  return Math.max(0, Math.min(100, (x / rect.width) * 100))
}

// 检查拖拽冲突
const isPeriodDragConflict = (roomId, periodIdx, startPercent, endPercent) => {
  const period = timePeriods[periodIdx]
  const startMin = (Math.min(startPercent, endPercent) / 100) * (period.endHour - period.startHour) * 60
  const endMin = (Math.max(startPercent, endPercent) / 100) * (period.endHour - period.startHour) * 60
  const startDate = new Date(currentDate.value + 'T00:00:00')
  startDate.setHours(period.startHour + Math.floor(startMin / 60), startMin % 60, 0, 0)
  const endDate = new Date(startDate)
  endDate.setMinutes(endDate.getMinutes() + (endMin - startMin))
  
  const bookings = roomBookings.value.filter(b => b.roomId === roomId && b.status !== '已取消')
  return bookings.some(b => {
    const bStart = new Date(b.startTime)
    const bEnd = new Date(b.endTime)
    return startDate < bEnd && endDate > bStart
  })
}

// 拖拽开始
const onDragStartPeriod = (event, room, periodIdx) => {
  event.preventDefault()
  const percent = getEventPeriodPercent(event, room.id, periodIdx)
  dragState.active = true
  dragState.roomId = room.id
  dragState.periodIdx = periodIdx
  dragState.startX = event.touches ? event.touches[0].clientX : event.clientX
  dragState.startPercent = percent
  dragState.endPercent = percent
  dragState.startStr = periodPercentToTimeStr(percent, periodIdx)
  dragState.endStr = periodPercentToTimeStr(percent, periodIdx)
}

// 拖拽移动
const onDragMovePeriod = (event, room, periodIdx) => {
  if (!dragState.active || dragState.roomId !== room.id || dragState.periodIdx !== periodIdx) return
  event.preventDefault()
  const percent = getEventPeriodPercent(event, room.id, periodIdx)
  dragState.endPercent = percent
  dragState.endStr = periodPercentToTimeStr(percent, periodIdx)
}

// 拖拽结束
const onDragEndPeriod = (event, room) => {
  if (!dragState.active || dragState.roomId !== room.id) return
  
  const periodIdx = dragState.periodIdx
  const startPct = Math.min(dragState.startPercent, dragState.endPercent)
  const endPct = Math.max(dragState.startPercent, dragState.endPercent)
  
  if (endPct - startPct < 1) {
    dragState.active = false
    return
  }
  
  const startStr = periodPercentToTimeStr(startPct, periodIdx)
  const endStr = periodPercentToTimeStr(endPct, periodIdx)
  
  const hasConflict = isPeriodDragConflict(room.id, periodIdx, startPct, endPct)
  
  dragState.active = false
  
  if (hasConflict) {
    showToast('所选时段与已有预约冲突')
    return
  }
  
  router.push({
    path: `/meeting/reserve/${room.id}`,
    query: {
      roomName: room.name,
      date: currentDate.value,
      startTime: startStr,
      endTime: endStr
    }
  })
}

// 计算拖拽块样式
const getPeriodDragStyle = (periodIdx) => {
  const left = Math.min(dragState.startPercent, dragState.endPercent)
  const width = Math.abs(dragState.endPercent - dragState.startPercent)
  return {
    left: left + '%',
    width: width + '%'
  }
}

// 显示预约详情
const showBookingInfo = (booking) => {
  const start = new Date(booking.startTime)
  const end = new Date(booking.endTime)
  const timeStr = `${String(start.getHours()).padStart(2,'0')}:${String(start.getMinutes()).padStart(2,'0')}-${String(end.getHours()).padStart(2,'0')}:${String(end.getMinutes()).padStart(2,'0')}`
  showToast(`${timeStr} | ${booking.title} | ${booking.organizerName}`)
}

// =============================================
// ===== 时间选择列（修改预订用） =====
// =============================================
const generateEditTimeColumns = () => {
  const columns = []
  for (let hour = 8; hour <= 21; hour++) {
    for (let minute = 0; minute < 60; minute += 5) {
      const time = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
      columns.push({ text: time, value: time })
    }
  }
  return columns
}
const timeColumns = generateEditTimeColumns()

// =============================================
// ===== 工具方法 =====
// =============================================
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return dateStr
  }
}

const getEquipmentList = (equipment) => {
  if (!equipment) return []
  if (typeof equipment === 'string') {
    return equipment.split(',').map(e => e.trim())
  }
  if (Array.isArray(equipment)) {
    return equipment
  }
  return []
}

// =============================================
// ===== 加载数据 =====
// =============================================
const loadRoomList = async () => {
  try {
    const res = await getMeetingRoomList()
    if (res.code === 0) {
      roomList.value = res.data || []
      updateRoomStatus()
    }
  } catch (error) {
    console.error('加载会议室列表失败:', error)
    showToast('加载会议室失败')
  }
}

const loadMyBookings = async () => {
  try {
    const id = localStorage.getItem('employeeId')
    if (!id) return
    const res = await getMyMeetings(parseInt(id))
    if (res.code === 0) {
      myBookings.value = res.data || []
    }
  } catch (error) {
    console.error('加载我的预订失败:', error)
  }
}

const loadAllRoomBookings = async () => {
  try {
    const promises = roomList.value.map(room => getRoomMeetings(room.id))
    const results = await Promise.all(promises)
    const allBookings = []
    results.forEach(res => {
      if (res.code === 0 && res.data) {
        allBookings.push(...res.data)
      }
    })
    roomBookings.value = allBookings
  } catch (error) {
    console.error('加载会议室预订失败:', error)
  }
}

const loadRoomBookings = async (roomId) => {
  try {
    const res = await getRoomMeetings(roomId)
    if (res.code === 0) {
      const others = roomBookings.value.filter(b => b.roomId !== roomId)
      roomBookings.value = [...others, ...(res.data || [])]
    }
  } catch (error) {
    console.error('加载会议室预订失败:', error)
  }
}

// =============================================
// ===== 更新会议室状态 =====
// =============================================
const updateRoomStatus = () => {
  let free = 0
  let busy = 0
  roomList.value.forEach(room => {
    if (room.status === '可用') {
      free++
    } else {
      busy++
    }
  })
  stats.value.free = free
  stats.value.busy = busy
}

// =============================================
// ===== 日期 =====
// =============================================
const minDate = computed(() => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
})

const onConfirmDate = async (value) => {
  const date = new Date(value)
  currentDate.value = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
  showCalendar.value = false
  await loadAllRoomBookings()
  await checkAllRoomFull()
}

const checkAllRoomFull = async () => {
  roomFullMap.value = {}
  for (const room of roomList.value) {
    roomFullMap.value[room.id] = await isRoomFull(room.id)
  }
}

const isRoomFull = async (roomId) => {
  try {
    const res = await checkRoomFull(roomId, currentDate.value)
    return res.code === 0 && res.data === true
  } catch {
    return false
  }
}

// =============================================
// ===== 预订 =====
// =============================================
const goReserve = (room) => {
  router.push({
    path: `/meeting/reserve/${room.id}`,
    query: {
      roomName: room.name,
      date: currentDate.value || new Date().toISOString().split('T')[0]
    }
  })
}

const goControl = (meeting) => {
  router.push({
    path: `/meeting/control/${meeting.id}`,
    query: {
      roomId: meeting.roomId
    }
  })
}

// =============================================
// ===== 我的预订 - 取消/修改 =====
// =============================================
const showEditBooking = ref(false)
const editBookingId = ref(null)
const editSubmitting = ref(false)
const editConflictDetected = ref(false)
const showEditStartPicker = ref(false)
const showEditEndPicker = ref(false)

const editForm = ref({
  title: '',
  startTime: '',
  endTime: '',
  participants: '',
  participantIds: '',
  participantsText: ''
})

const showEditParticipantPicker = ref(false)
const editParticipantSearchKey = ref('')
const editSelectedParticipantIds = ref([])

const filteredEditEmployees = computed(() => {
  if (!editParticipantSearchKey.value) return allAdminEmployees.value
  const key = editParticipantSearchKey.value.toLowerCase()
  return allAdminEmployees.value.filter(emp => {
    const nameMatch = emp.name && emp.name.toLowerCase().includes(key)
    const deptMatch = emp.departmentName && emp.departmentName.toLowerCase().includes(key)
    return nameMatch || deptMatch
  })
})

const confirmEditParticipants = () => {
  const selectedEmps = allAdminEmployees.value.filter(emp => 
    editSelectedParticipantIds.value.includes(String(emp.id))
  )
  editForm.value.participantsText = selectedEmps.map(e => e.name).join('，')
  editForm.value.participantIds = editSelectedParticipantIds.value.join(',')
  editForm.value.participants = selectedEmps.map(e => e.name).join(',')
  showEditParticipantPicker.value = false
}

const editBooking = async (item) => {
  editBookingId.value = item.id
  editForm.value = {
    title: item.title,
    startTime: formatTimeOnly(item.startTime),
    endTime: formatTimeOnly(item.endTime),
    participants: item.participants || '',
    participantIds: item.participantIds || '',
    participantsText: item.participants || ''
  }
  
  if (allAdminEmployees.value.length === 0) {
    await loadAllEmployees()
  }
  
  if (item.participantIds) {
    editSelectedParticipantIds.value = item.participantIds.split(',').filter(id => id)
  } else {
    editSelectedParticipantIds.value = []
  }
  
  editConflictDetected.value = false
  showEditBooking.value = true
}

const formatTimeOnly = (dateStr) => {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return `${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch {
    return ''
  }
}

const cancelBooking = async (id) => {
  showConfirmDialog({
    title: '确认取消',
    message: '确定要取消该预订吗？',
    confirmButtonText: '确定取消',
    confirmButtonColor: '#ee0a24'
  }).then(async () => {
    try {
      const res = await cancelMeeting(id)
      if (res.code === 0) {
        showToast('已取消预订')
        await loadMyBookings()
        await loadRoomList()
      } else {
        showToast(res.msg || '取消失败')
      }
    } catch (error) {
      showToast('取消失败，请稍后重试')
    }
  }).catch(() => {})
}

const onConfirmEditStart = ({ selectedValues }) => {
  editForm.value.startTime = selectedValues[0]
  showEditStartPicker.value = false
  setTimeout(checkEditConflict, 100)
}

const onConfirmEditEnd = ({ selectedValues }) => {
  editForm.value.endTime = selectedValues[0]
  showEditEndPicker.value = false
  setTimeout(checkEditConflict, 100)
}

const checkEditConflict = () => {
  if (!editBookingId.value || !editForm.value.startTime || !editForm.value.endTime) {
    editConflictDetected.value = false
    return
  }
  
  const booking = myBookings.value.find(b => b.id === editBookingId.value)
  if (!booking) return
  
  const date = new Date(booking.startTime).toISOString().split('T')[0]
  const start = new Date(`${date}T${editForm.value.startTime}:00`)
  const end = new Date(`${date}T${editForm.value.endTime}:00`)
  
  const existing = roomBookings.value.filter(b => 
    b.roomId === booking.roomId && b.id !== editBookingId.value
  )
  
  const hasConflict = existing.some(b => {
    const bStart = new Date(b.startTime)
    const bEnd = new Date(b.endTime)
    return start < bEnd && end > bStart
  })
  
  editConflictDetected.value = hasConflict
}

const submitEditBooking = async () => {
  if (editConflictDetected.value) {
    showToast('该时段已被其他人预订，请选择其他时间')
    return
  }
  
  editSubmitting.value = true
  try {
    const booking = myBookings.value.find(b => b.id === editBookingId.value)
    if (!booking) return
    
    const date = new Date(booking.startTime).toISOString().split('T')[0]
    const data = {
      id: editBookingId.value,
      title: editForm.value.title,
      startTime: `${date}T${editForm.value.startTime}:00`,
      endTime: `${date}T${editForm.value.endTime}:00`,
      participants: editForm.value.participants,
      participantIds: editForm.value.participantIds
    }
    const res = await updateMeeting(data)
    if (res.code === 0) {
      showToast('预订已修改')
      showEditBooking.value = false
      await loadMyBookings()
      await loadRoomBookings(booking.roomId)
    } else {
      showToast(res.msg || '修改失败')
    }
  } catch (error) {
    showToast('修改失败，请稍后重试')
  } finally {
    editSubmitting.value = false
  }
}

// =============================================
// ===== 跳转详情 =====
// =============================================
const goDetail = (id) => {
  router.push(`/meeting/detail/${id}`)
}

// =============================================
// ===== 生命周期 =====
// =============================================
const updateCurrentTime = () => {
  now.value = new Date()
}

onMounted(async () => {
  const d = new Date()
  currentDate.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
  
  // 获取用户信息
  const id = localStorage.getItem('employeeId')
  if (id) {
    employeeId.value = parseInt(id)
  }
  employeeName.value = localStorage.getItem('name') || localStorage.getItem('username') || '用户'
  isAdmin.value = localStorage.getItem('role') === 'SYSTEM_ADMIN'
  
  // 加载数据
  loading.value = true
  await loadRoomList()
  await loadMyBookings()
  await loadMyInvited()
  await loadAllRoomBookings()
  loading.value = false
  
  timer = setInterval(updateCurrentTime, 60000)
  
  // 监听滚动事件
  window.addEventListener('scroll', handleScroll)
  // 初始检查
  setTimeout(handleScroll, 300)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.oa-meeting {
  max-width: 430px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  padding: 0 16px 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.06);
}

/* ===== 加载状态 ===== */
.loading-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.banner {
  background: linear-gradient(135deg, #6c5ce7 0%, #8c7cf7 100%);
  margin: 0 -16px 16px;
  padding: 36px 20px 28px;
  border-radius: 0 0 24px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.banner-title { font-size: 24px; font-weight: bold; color: #fff; margin: 0; }
.banner-desc { color: rgba(255,255,255,0.85); font-size: 14px; margin: 4px 0 0; }
.banner-icon {
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 16px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer;
}
.stat-card:active { transform: scale(0.95); }
.stat-label { font-size: 12px; color: #888; margin: 0; }
.stat-num { font-size: 28px; font-weight: bold; }

.date-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.date-text { flex: 1; font-size: 16px; color: #333; }

.room-list { display: flex; flex-direction: column; gap: 12px; }
.room-item {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.room-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; cursor: pointer; }
.room-info { display: flex; align-items: center; gap: 10px; }
.room-name { font-size: 17px; font-weight: bold; margin: 0; color: #222; }
.room-status { font-size: 12px; padding: 2px 10px; border-radius: 12px; }
.room-status.status-free { color: #00b894; background: #e8f8f0; }
.room-status.status-busy { color: #e17055; background: #ffe8e8; }
.room-capacity { color: #999; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.room-body { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; cursor: pointer; }
.room-location { color: #aaa; font-size: 13px; }
.room-equipment { display: flex; gap: 6px; }
.equip-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; background: #f0f0f0; color: #666; }
.room-footer { padding-top: 12px; border-top: 1px solid #f5f5f5; }

/* 连续时间轴甘特图（三行） */
.timeline-container { margin-bottom: 12px; display: flex; flex-direction: column; gap: 6px; }
.timeline-row { display: flex; align-items: stretch; }
.period-label {
  width: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  font-weight: 500;
}
.period-content { flex: 1; display: flex; flex-direction: column; }
.timeline-ruler {
  position: relative;
  height: 16px;
  font-size: 9px;
  color: #bbb;
}
.ruler-mark {
  position: absolute;
  transform: translateX(-50%);
  white-space: nowrap;
}
.timeline-track {
  position: relative;
  height: 36px;
  background: #f0f7ff;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
}
.booked-block {
  position: absolute;
  top: 2px;
  bottom: 2px;
  background: linear-gradient(135deg, #6c5ce7 0%, #8c7cf7 100%);
  border-radius: 5px;
  display: flex;
  align-items: center;
  padding: 0 6px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(108, 92, 231, 0.2);
}
.block-label {
  font-size: 10px;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.drag-block {
  position: absolute;
  top: 2px;
  bottom: 2px;
  background: rgba(0, 184, 148, 0.3);
  border: 2px dashed #00b894;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.drag-label {
  font-size: 9px;
  color: #00b894;
  font-weight: 600;
  white-space: nowrap;
}
.book-btn-container { display: flex; justify-content: center; }
.book-btn { 
  height: 40px; 
  padding: 0 32px; 
  font-size: 15px; 
  background: linear-gradient(135deg, #6c5ce7 0%, #8c7cf7 100%); 
  border: none; 
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.3);
  transition: all 0.2s ease;
}
.book-btn:active { 
  transform: scale(0.96); 
  box-shadow: 0 2px 6px rgba(108, 92, 231, 0.2);
}
.book-btn:disabled { 
  opacity: 0.5; 
  box-shadow: none;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #ccc;
}
.empty-state p { margin-top: 8px; font-size: 14px; }

.reserve-room-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.reserve-room-name { font-size: 16px; font-weight: 500; color: #222; }
.reserve-room-location { font-size: 13px; color: #999; }

.conflict-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fff0e8;
  border-radius: 8px;
  margin-top: 12px;
  font-size: 13px;
  color: #e17055;
}

.booking-list { display: flex; flex-direction: column; gap: 12px; }
.booking-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 14px 16px;
}
.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.booking-room { font-size: 15px; font-weight: 500; color: #222; }
.booking-status { font-size: 12px; padding: 2px 12px; border-radius: 12px; }
.booking-status.confirmed { color: #00b894; background: #e8f8f0; }
.booking-status.cancelled { color: #e17055; background: #fff0e8; }
.booking-status.active-status { color: #6c5ce7; background: #f0e8ff; }
.booking-status.ended-status { color: #999; background: #f5f5f5; }
.booking-search { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.search-date-btn { 
  display: flex; align-items: center; gap: 4px; 
  white-space: nowrap; font-size: 13px; color: #6c5ce7;
  padding: 6px 12px; border: 1px solid #ddd; border-radius: 20px;
  flex-shrink: 0; cursor: pointer;
}
.history-item { opacity: 0.7; }
.invited-item { cursor: pointer; transition: transform 0.1s; }
.invited-item:active { transform: scale(0.98); }
.booking-organizer { font-size: 12px; color: #aaa; }
.booking-body { display: flex; flex-direction: column; gap: 2px; margin-bottom: 10px; }
.booking-title { font-size: 14px; color: #333; }
.booking-time { font-size: 12px; color: #999; }
.booking-footer {
  display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.booking-participants { font-size: 13px; color: #888; }
.booking-actions { display: flex; gap: 6px; }

.bottom-bar { padding: 16px 0 8px; }
.back-btn {
  border-radius: 12px !important;
  height: 48px !important;
  font-size: 16px !important;
  border-color: #6c5ce7 !important;
  color: #6c5ce7 !important;
}
.back-btn:active { background: #f0e8ff !important; }

.safe-bottom { height: 20px; }
:deep(.van-popup) { border-radius: 16px 16px 0 0; }

/* ============================================= */
/* ===== 一键到顶 / 一键到底 浮动按钮 ===== */
/* ============================================= */
.scroll-buttons {
  position: fixed;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 999;
  animation: fadeIn 0.3s ease;
}

.scroll-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 16px rgba(108, 92, 231, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid rgba(108, 92, 231, 0.15);
}

.scroll-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(108, 92, 231, 0.35);
}

.scroll-btn:active {
  transform: scale(0.92);
}

.scroll-btn span {
  font-size: 10px;
  color: #6c5ce7;
  margin-top: -2px;
  font-weight: 500;
}

/* 会议室管理列表样式 */
.room-manage-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.room-manage-item {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 12px 14px;
}
.room-manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.room-manage-name {
  font-size: 15px;
  font-weight: 500;
  color: #222;
}
.room-manage-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
}
.room-manage-status.status-free {
  color: #00b894;
  background: #e8f8f0;
}
.room-manage-status.status-busy {
  color: #e17055;
  background: #ffe8e8;
}
.room-manage-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}
.room-manage-equip {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}
.room-manage-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* 搜索栏样式 */
.search-wrap {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
}

.scroll-btn .van-icon {
  line-height: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 当滚动到顶部时，顶部按钮淡出 */
.scroll-btn[data-hidden="true"] {
  opacity: 0;
  pointer-events: none;
  transform: scale(0.8);
}
</style>